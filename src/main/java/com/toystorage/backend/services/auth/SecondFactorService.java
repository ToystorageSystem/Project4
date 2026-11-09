package com.toystorage.backend.services.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecondFactorService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public boolean enabled(long userId) {
        return !jdbc.query("SELECT secret FROM user_totp WHERE user_id = ?",
                (rs, row) -> rs.getString(1), userId).isEmpty();
    }

    public String createSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        StringBuilder result = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 255);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                result.append(ALPHABET.charAt((buffer >> bits) & 31));
            }
        }
        return result.toString();
    }

    public String uri(String email, String secret) {
        return "otpauth://totp/ToyStorage:" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "?secret=" + secret + "&issuer=ToyStorage&digits=6&period=30";
    }

    private byte[] decode(String secret) {
        int buffer = 0, bits = 0;
        byte[] result = new byte[secret.length() * 5 / 8];
        int offset = 0;
        for (char c : secret.toCharArray()) {
            buffer = (buffer << 5) | ALPHABET.indexOf(c);
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                result[offset++] = (byte) (buffer >> bits);
            }
        }
        return result;
    }

    private boolean matches(String secret, String code, long counter) {
        if (code == null || !code.matches("[0-9]{6}")) return false;
        try {
            byte[] input = java.nio.ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
            byte[] hmac = mac.doFinal(input);
            int offset = hmac[hmac.length - 1] & 15;
            int value = ((hmac[offset] & 127) << 24) |
                    ((hmac[offset + 1] & 255) << 16) |
                    ((hmac[offset + 2] & 255) << 8) |
                    (hmac[offset + 3] & 255);
            return java.security.MessageDigest.isEqual(
                    String.format("%06d", value % 1000000).getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to verify code", e);
        }
    }

    public boolean valid(String secret, String code) {
        long now = Instant.now().getEpochSecond() / 30;
        for (long step = now - 1; step <= now + 1; step++)
            if (matches(secret, code, step)) return true;
        return false;
    }

    @Transactional
    public boolean verify(long userId, String code) {
        var rows = jdbc.query("SELECT secret, last_counter FROM user_totp WHERE user_id = ? FOR UPDATE",
                (rs, row) -> new Object[]{rs.getString(1), rs.getLong(2)}, userId);
        if (rows.isEmpty()) return false;
        String secret = (String) rows.get(0)[0];
        long last = (long) rows.get(0)[1];
        long now = Instant.now().getEpochSecond() / 30;
        for (long step = now - 1; step <= now + 1; step++) {
            if (step > last && matches(secret, code, step)) {
                jdbc.update("UPDATE user_totp SET last_counter = ? WHERE user_id = ?", step, userId);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void enroll(long userId, String secret, String code) {
        if (enabled(userId) || !valid(secret, code))
            throw new IllegalArgumentException("Invalid code or two-factor authentication already enabled");
        jdbc.update("INSERT INTO user_totp (user_id, secret, last_counter) VALUES (?, ?, ?)",
                userId, secret, Instant.now().getEpochSecond() / 30);
    }
}
