package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.services.auth.SecondFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthProtectionController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecondFactorService secondFactor;

    private Users current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body,
                                                        HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Authentication pending = (Authentication) session.getAttribute("PENDING_2FA_AUTH");
        Long id = (Long) session.getAttribute("PENDING_2FA_ID");
        Long expires = (Long) session.getAttribute("PENDING_2FA_EXPIRES");
        Integer attempts = (Integer) session.getAttribute("PENDING_2FA_ATTEMPTS");
        if (pending == null || id == null || expires == null || System.currentTimeMillis() > expires
                || (attempts != null && attempts >= 5)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        }
        session.setAttribute("PENDING_2FA_ATTEMPTS", attempts == null ? 1 : attempts + 1);
        if (!secondFactor.verify(id, body.get("code")))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
        session.removeAttribute("PENDING_2FA_AUTH");
        session.removeAttribute("PENDING_2FA_ID");
        session.removeAttribute("PENDING_2FA_EXPIRES");
        session.removeAttribute("PENDING_2FA_ATTEMPTS");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(pending);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        Users user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(Map.of("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()),
                "requiresTwoFactorSetup", false));
    }

    @GetMapping("/2fa/setup")
    public Map<String, String> setup(HttpServletRequest request) {
        Users user = current();
        if (Boolean.TRUE.equals(user.getMustChangePassword()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Change password first");
        if (secondFactor.enabled(user.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enabled");
        HttpSession session = request.getSession(true);
        String secret = (String) session.getAttribute("PENDING_2FA_SECRET");
        if (secret == null) {
            secret = secondFactor.createSecret();
            session.setAttribute("PENDING_2FA_SECRET", secret);
        }
        return Map.of("secret", secret, "otpauthUri", secondFactor.uri(user.getEmail(), secret));
    }

    @PostMapping("/2fa/setup/confirm")
    public ResponseEntity<Void> confirm(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Users user = current();
        if (Boolean.TRUE.equals(user.getMustChangePassword()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Change password first");
        HttpSession session = request.getSession(false);
        String secret = session == null ? null : (String) session.getAttribute("PENDING_2FA_SECRET");
        if (secret == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start setup first");
        try {
            secondFactor.enroll(user.getId(), secret, body.get("code"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        session.removeAttribute("PENDING_2FA_SECRET");
        return ResponseEntity.noContent().build();
    }
}
