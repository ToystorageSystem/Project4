package com.toystorage.backend.services.auth;
import com.toystorage.backend.dto.request.auth.PasswordResetRequests;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.entity.auth.PasswordResetToken;
import com.toystorage.backend.repository.auth.*;
import com.toystorage.backend.repository.users.UserRepository;
import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = ResponseStatusException.class)
public class PasswordResetService {
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(PasswordResetService.class);
    private final UserRepository userRepository;
    private final AuthUserLockRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetMailer mailer;
    private final AuthService auth;
    private final EntityManager entityManager;

    public void forgot(String email) {
        if (!mailer.isReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RESET_EMAIL_NOT_CONFIGURED");
        var found=userRepository.findByEmail(email.trim());
        if (found.isEmpty()) return;
        var user=users.lockById(found.get().getId()).orElse(null);
        if (user==null) return;
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.getStatus()==null || !"ACTIVE".equals(user.getStatus().name())) return;
        Instant now=Instant.now();
        if (tokens.countByUserIdAndCreatedAtAfter(user.getId(),now.minusSeconds(3600))>=3) return;
        var last=tokens.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (last.isPresent() && last.get().getCreatedAt().isAfter(now.minusSeconds(60))) return;
        tokens.invalidateAll(user.getId(),now);
        byte[] random=new byte[32]; RANDOM.nextBytes(random);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        PasswordResetToken token=new PasswordResetToken();
        token.setUserId(user.getId()); token.setTokenHash(hash(raw));
        token.setPasswordFingerprint(hash(user.getPassword()));
        token.setCreatedAt(now); token.setExpiresAt(now.plusSeconds(900));
        tokens.saveAndFlush(token);
        String destination=user.getEmail();
        Runnable delivery=() -> {
            try { mailer.send(destination,raw); }
            catch (RuntimeException ignored) {
                // Never log the exception/email body: a mail failure may contain the token or credentials.
                LOG.error("Password reset email delivery failed; check SMTP configuration and connectivity");
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { delivery.run(); }
            });
        } else { delivery.run(); }
    }

    public LoginResponse reset(PasswordResetRequests.Reset body, HttpServletRequest http) {
        String digest=hash(body.token());
        var candidate=tokens.findByTokenHash(digest).orElseThrow(PasswordResetService::invalid);
        var user=users.lockById(candidate.getUserId()).orElseThrow(PasswordResetService::invalid);
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (user.getStatus()==null || !"ACTIVE".equals(user.getStatus().name())) throw invalid();
        // All paths lock user first, then token: serialize resets and avoid inverse lock ordering.
        var token=tokens.lockById(candidate.getId()).orElseThrow(PasswordResetService::invalid);
        entityManager.refresh(token,LockModeType.PESSIMISTIC_WRITE);
        if (!digest.equals(token.getTokenHash()) || !user.getId().equals(token.getUserId())
                || token.getUsedAt()!=null || !token.getExpiresAt().isAfter(Instant.now())
                || !hash(user.getPassword()).equals(token.getPasswordFingerprint())) throw invalid();
        LoginResponse result=auth.resetPasswordAndEnroll(user,body.newPassword(),body.otp(),body.recoveryCode(),http);
        token.setUsedAt(Instant.now());
        entityManager.flush();
        tokens.invalidateAll(user.getId(),token.getUsedAt());
        return result;
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"RESET_TOKEN_INVALID_OR_EXPIRED");
    }
}
