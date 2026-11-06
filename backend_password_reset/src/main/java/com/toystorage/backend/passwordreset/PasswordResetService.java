package com.toystorage.backend.passwordreset;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "app.password-reset.enabled", havingValue = "true")
public class PasswordResetService {
    private final JdbcTemplate jdbc;
    private final JavaMailSender mail;
    private final PasswordEncoder encoder;
    private final String from;
    private final SecureRandom random = new SecureRandom();
    public PasswordResetService(JdbcTemplate jdbc, JavaMailSender mail, PasswordEncoder encoder,
            @Value("${app.password-reset.from}") String from) {
        this.jdbc = jdbc; this.mail = mail; this.encoder = encoder; this.from = from;
    }
    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @Transactional
    public void request(String email) {
        // Lock the account before issuing: concurrent requests cannot bypass cooldown.
        var users = jdbc.queryForList("SELECT id, email FROM users WHERE email = ? AND status = 'ACTIVE' FOR UPDATE", email);
        if (users.isEmpty()) return;
        long id = ((Number) users.get(0).get("id")).longValue();
        var prior = jdbc.queryForList("SELECT issued_at FROM password_reset_tokens WHERE user_id = ?", id);
        Instant now = Instant.now();
        if (!prior.isEmpty() && ((Timestamp) prior.get(0).get("issued_at")).toInstant().isAfter(now.minusSeconds(60))) return;
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("""
            INSERT INTO password_reset_tokens (user_id, token_hash, issued_at, expires_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE token_hash=VALUES(token_hash), issued_at=VALUES(issued_at), expires_at=VALUES(expires_at)
            """, id, hash(token), Timestamp.from(now), Timestamp.from(now.plusSeconds(900)));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo((String) users.get(0).get("email"));
        message.setSubject("Toy Storage System - Khoi phuc mat khau");
        message.setText("Ma khoi phuc mat khau cua ban:\n\n" + token
                + "\n\nMa co hieu luc 15 phut va chi dung mot lan. Neu ban khong yeu cau, hay bo qua email nay.");
        mail.send(message); // Failure rolls back the token; never log plaintext tokens.
    }
    @Transactional
    public void reset(String email, String token, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mat khau vuot qua gioi han 72 byte UTF-8.");
        }
        var rows = jdbc.queryForList("""
            SELECT t.user_id, t.token_hash, t.expires_at FROM password_reset_tokens t
            JOIN users u ON u.id=t.user_id WHERE u.email=? AND u.status='ACTIVE' FOR UPDATE
            """, email);
        if (rows.isEmpty()) throw invalid();
        var row = rows.get(0);
        if (!((Timestamp) row.get("expires_at")).toInstant().isAfter(Instant.now())
                || !MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
                row.get("token_hash").toString().getBytes(StandardCharsets.US_ASCII))) throw invalid();
        jdbc.update("UPDATE users SET password=?, must_change_password=false, updated_at=? WHERE id=?",
                encoder.encode(newPassword), Timestamp.from(Instant.now()), row.get("user_id"));
        jdbc.update("DELETE FROM password_reset_tokens WHERE user_id=?", row.get("user_id"));
    }
    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma khoi phuc khong hop le hoac da het han.");
    }
}
