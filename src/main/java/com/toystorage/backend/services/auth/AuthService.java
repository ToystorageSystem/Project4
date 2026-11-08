package com.toystorage.backend.services.auth;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.config.security.CustomUserDetailsService;
import com.toystorage.backend.dto.request.auth.LoginRequest;
import com.toystorage.backend.dto.request.auth.MfaRequests;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.repository.auth.AuthUserLockRepository;
import com.toystorage.backend.services.auth.mfa.MfaCrypto;
import com.toystorage.backend.services.auth.mfa.Totp;
import com.toystorage.backend.utils.QRCodeUtil;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = ResponseStatusException.class)
public class AuthService {
    private static final String PENDING = AuthService.class.getName() + ".pending";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService detailsService;
    private final AuthUserLockRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MfaCrypto crypto;
    private final SessionAuthenticationStrategy loginSessionStrategy;
    private final SessionRegistry sessionRegistry;
    private final jakarta.persistence.EntityManager entityManager;

    public enum Stage { CHANGE_PASSWORD, SETUP_2FA, VERIFY_2FA, AUTHENTICATED }
    private record Pending(Long userId, String passwordHash, Stage stage,
                           Instant expiresAt, String setupSecret, String previousSecret) implements Serializable {}
    public record SetupResponse(String qrDataUrl, String manualKey, String otpauthUri) {}

    public LoginResponse login(LoginRequest request, HttpServletRequest http) {
        HttpSession old = http.getSession(false);
        if (old != null) old.invalidate();
        SecurityContextHolder.clearContext();
        Authentication verified = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        CustomUserDetails principal = (CustomUserDetails) verified.getPrincipal();
        String verifiedPasswordHash = principal.getPassword();
        Users user = lock(principal.getId());
        if (!user.getPassword().equals(verifiedPasswordHash)) throw unauthorized();
        Stage stage = Boolean.TRUE.equals(user.getMustChangePassword()) ? Stage.CHANGE_PASSWORD
                : !user.isTwoFactorEnabled() ? Stage.SETUP_2FA : Stage.VERIFY_2FA;
        HttpSession session = http.getSession(true);
        session.setMaxInactiveInterval(300);
        session.setAttribute(PENDING, new Pending(user.getId(), user.getPassword(), stage,
                Instant.now().plusSeconds(300), null, null));
        return response(user, stage, Set.of(), null);
    }

    public LoginResponse status(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null && session.getAttribute(PENDING) instanceof Pending) {
            Pending p = pending(http, null);
            return response(current(p), p.stage(), Set.of(), null);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails p)) throw unauthorized();
        Users user = lock(p.getId());
        if (!user.getPassword().equals(p.getPassword())) throw unauthorized();
        return response(user, Stage.AUTHENTICATED, authorityNames(auth), null);
    }

    public LoginResponse changePassword(MfaRequests.ChangePassword body, HttpServletRequest http) {
        Pending p = pending(http, Stage.CHANGE_PASSWORD);
        Users user = current(p);
        checkLock(user);
        if (!passwordEncoder.matches(body.currentPassword(), user.getPassword())) fail(user);
        if (body.newPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || passwordEncoder.matches(body.newPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NEW_PASSWORD_INVALID");
        user.setPassword(passwordEncoder.encode(body.newPassword()));
        user.setMustChangePassword(false);
        clearFailures(user);
        expireOtherSessions(user.getId());
        Stage next = !user.isTwoFactorEnabled() ? Stage.SETUP_2FA : Stage.VERIFY_2FA;
        http.changeSessionId();
        http.getSession().setAttribute(PENDING, new Pending(user.getId(), user.getPassword(), next, p.expiresAt(), null, null));
        return response(user, next, Set.of(), null);
    }

    public SetupResponse setup(HttpServletRequest http) {
        Pending p = pending(http, Stage.SETUP_2FA);
        Users user = current(p);
        checkLock(user);
        if (!mayEnroll(p, user)) throw unauthorized();
        String secret = p.setupSecret() == null ? Totp.newSecret() : p.setupSecret();
        http.getSession().setAttribute(PENDING, new Pending(p.userId(), p.passwordHash(), p.stage(), p.expiresAt(), secret, p.previousSecret()));
        String uri = "otpauth://totp/" + encode("ToyStorage:" + user.getEmail())
                + "?secret=" + secret + "&issuer=ToyStorage&algorithm=SHA1&digits=6&period=30";
        try { return new SetupResponse("data:image/png;base64," + QRCodeUtil.generateQRImage(uri), secret, uri); }
        catch (Exception e) { throw new IllegalStateException("Cannot generate QR", e); }
    }

    public LoginResponse confirm(String code, HttpServletRequest http, HttpServletResponse output) {
        Pending p = pending(http, Stage.SETUP_2FA);
        Users user = current(p);
        checkLock(user);
        if (!mayEnroll(p, user) || p.setupSecret() == null) throw unauthorized();
        long step = Totp.verify(p.setupSecret(), code, Instant.now().getEpochSecond(), -1);
        if (step < 0) fail(user);
        user.setTotpSecret(crypto.encrypt(user.getId(), p.setupSecret()));
        user.setTwoFactorEnabled(true);
        user.setMfaReenrollRequired(false);
        user.setMfaLastUsedStep(step);
        List<String> codes = newRecoveryCodes(user);
        clearFailures(user);
        return finish(user, http, output, codes);
    }

    public LoginResponse verify(String code, HttpServletRequest http, HttpServletResponse output) {
        Pending p = pending(http, Stage.VERIFY_2FA);
        Users user = current(p);
        checkLock(user);
        if (!user.isTwoFactorEnabled() || user.getTotpSecret() == null) throw unauthorized();
        long step = Totp.verify(readSecret(user), code,
                Instant.now().getEpochSecond(), user.getMfaLastUsedStep());
        if (step < 0) fail(user);
        user.setMfaLastUsedStep(step);
        if (user.isMfaReenrollRequired()) {
            clearFailures(user);
            return beginEnrollment(user, http);
        }
        // Preserve existing Authenticator enrollment; encrypt its legacy Base32 key after a valid OTP.
        if (!user.getTotpSecret().startsWith("v1:"))
            user.setTotpSecret(crypto.encrypt(user.getId(), readSecret(user)));
        List<String> codes = user.getMfaRecoveryHashes() == null ? newRecoveryCodes(user) : null;
        clearFailures(user);
        return finish(user, http, output, codes);
    }

    public LoginResponse recover(String code, HttpServletRequest http) {
        Pending p = pending(http, Stage.VERIFY_2FA);
        Users user = current(p);
        checkLock(user);
        consumeRecoveryCode(user, code);
        clearFailures(user);
        expireOtherSessions(user.getId());
        http.changeSessionId();
        http.getSession().setAttribute(PENDING, new Pending(p.userId(), p.passwordHash(), Stage.SETUP_2FA, p.expiresAt(), null, user.getTotpSecret()));
        return response(user, Stage.SETUP_2FA, Set.of(), null);
    }

    /** Called only after PasswordResetService has locked and verified the email token. */
    public LoginResponse resetPasswordAndEnroll(Users user, String newPassword,
            String otp, String recoveryCode, HttpServletRequest http) {
        checkLock(user);
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || passwordEncoder.matches(newPassword, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NEW_PASSWORD_INVALID");
        if (user.isTwoFactorEnabled()) {
            boolean hasOtp = otp != null && !otp.isBlank();
            boolean hasRecovery = recoveryCode != null && !recoveryCode.isBlank();
            if (hasOtp == hasRecovery)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "SECOND_FACTOR_REQUIRED");
            if (hasOtp) {
                long step = Totp.verify(readSecret(user), otp, Instant.now().getEpochSecond(), user.getMfaLastUsedStep());
                if (step < 0) fail(user);
                user.setMfaLastUsedStep(step);
            } else {
                consumeRecoveryCode(user, recoveryCode);
            }
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setMfaReenrollRequired(true);
        clearFailures(user);
        expireOtherSessions(user.getId());
        return beginEnrollment(user, http);
    }

    private void consumeRecoveryCode(Users user, String code) {
        String digest = hash(code.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT));
        boolean valid = user.getMfaRecoveryHashes() != null && Arrays.stream(user.getMfaRecoveryHashes().split("\n"))
                .anyMatch(value -> MessageDigest.isEqual(value.getBytes(StandardCharsets.US_ASCII),
                        digest.getBytes(StandardCharsets.US_ASCII)));
        if (!valid) fail(user);
        // Keep the old factor enabled until this recovery-authorized session confirms a replacement.
        // A separate password-only login must never gain enrollment access during recovery.
        user.setMfaRecoveryHashes(Arrays.stream(user.getMfaRecoveryHashes().split("\n"))
                .filter(value -> !value.equals(digest)).collect(Collectors.joining("\n")));
    }

    private LoginResponse beginEnrollment(Users user, HttpServletRequest http) {
        HttpSession old = http.getSession(false);
        if (old != null) old.invalidate();
        SecurityContextHolder.clearContext();
        HttpSession session = http.getSession(true);
        session.setMaxInactiveInterval(300);
        session.setAttribute(PENDING, new Pending(user.getId(), user.getPassword(), Stage.SETUP_2FA,
                Instant.now().plusSeconds(300), null, user.isTwoFactorEnabled() ? user.getTotpSecret() : null));
        return response(user, Stage.SETUP_2FA, Set.of(), null);
    }

    private LoginResponse finish(Users user, HttpServletRequest http, HttpServletResponse output, List<String> recoveryCodes) {
        CustomUserDetails details = (CustomUserDetails) detailsService.loadUserByUsername(user.getEmail());
        if (!details.isEnabled() || user.isMfaReenrollRequired()) throw unauthorized();
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
        entityManager.flush();
        loginSessionStrategy.onAuthentication(auth, http, output);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, http, output);
        HttpSession session = http.getSession();
        session.removeAttribute(PENDING);
        session.setMaxInactiveInterval(900);
        return response(user, Stage.AUTHENTICATED, authorityNames(auth), recoveryCodes);
    }

    private Pending pending(HttpServletRequest http, Stage expected) {
        HttpSession session = http.getSession(false);
        if (session == null || !(session.getAttribute(PENDING) instanceof Pending p)) throw unauthorized();
        if (!p.expiresAt().isAfter(Instant.now())) { session.invalidate(); throw unauthorized(); }
        if (expected != null && p.stage() != expected)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "WRONG_AUTH_STAGE");
        return p;
    }
    private boolean mayEnroll(Pending p, Users user) {
        return Objects.equals(user.isTwoFactorEnabled() ? user.getTotpSecret() : null, p.previousSecret());
    }
    private List<String> newRecoveryCodes(Users user) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            byte[] value = new byte[16]; RANDOM.nextBytes(value);
            codes.add(HexFormat.of().withUpperCase().formatHex(value));
        }
        user.setMfaRecoveryHashes(codes.stream().map(AuthService::hash).collect(Collectors.joining("\n")));
        return codes;
    }
    private String readSecret(Users user) {
        String value = user.getTotpSecret();
        if (value != null && value.startsWith("v1:")) return crypto.decrypt(user.getId(), value);
        if (value != null) {
            String legacy = value.replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
            if (legacy.matches("[A-Z2-7]{16,64}")) return legacy;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "INVALID_2FA_CONFIGURATION");
    }
    private Users current(Pending p) {
        Users user = lock(p.userId());
        if (!user.getPassword().equals(p.passwordHash())) throw unauthorized();
        if (Boolean.TRUE.equals(user.getMustChangePassword()) && p.stage() != Stage.CHANGE_PASSWORD) throw unauthorized();
        return user;
    }
    private Users lock(Long id) {
        Users user = users.lockById(id).orElseThrow(AuthService::unauthorized);
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (user.getStatus() == null || !"ACTIVE".equals(user.getStatus().name())) throw unauthorized();
        return user;
    }
    private void checkLock(Users user) {
        if (user.getMfaLockedUntil() != null && user.getMfaLockedUntil().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP_LOCKED_5_MINUTES");
    }
    private void fail(Users user) {
        int attempts = user.getMfaFailedAttempts() + 1;
        user.setMfaFailedAttempts(attempts);
        if (attempts >= 5) { user.setMfaFailedAttempts(0); user.setMfaLockedUntil(Instant.now().plusSeconds(300)); }
        throw new ResponseStatusException(attempts >= 5 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED, "INVALID_VERIFICATION");
    }
    private void clearFailures(Users user) { user.setMfaFailedAttempts(0); user.setMfaLockedUntil(null); }
    private void expireOtherSessions(Long id) {
        for (Object principal : sessionRegistry.getAllPrincipals())
            if (principal instanceof CustomUserDetails p && id.equals(p.getId()))
                sessionRegistry.getAllSessions(principal, false).forEach(s -> s.expireNow());
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED"); }
    private static Set<String> authorityNames(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
    private static LoginResponse response(Users user, Stage stage, Set<String> authorities, List<String> codes) {
        return LoginResponse.builder().id(user.getId()).userCode(user.getUserCode()).name(user.getName())
                .email(user.getEmail()).authorities(authorities).stage(stage.name())
                .mustChangePassword(stage == Stage.CHANGE_PASSWORD).twoFactorRequired(stage == Stage.VERIFY_2FA)
                .twoFactorSetupRequired(stage == Stage.SETUP_2FA).recoveryCodes(codes).build();
    }
}
