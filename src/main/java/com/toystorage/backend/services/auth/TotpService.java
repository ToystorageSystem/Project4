
package com.toystorage.backend.services.auth;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int SECRET_LENGTH_BYTES = 20;

    private static final String BASE32_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static final String ISSUER = "ToyStorageSystem";

    private final SecureRandom secureRandom =
            new SecureRandom();

    // Tao secret ngau nhien cho moi tai khoan.
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_LENGTH_BYTES];
        secureRandom.nextBytes(bytes);

        return encodeBase32(bytes);
    }

    // Tao URI de sinh ma QR cho Google Authenticator.
    public String createOtpAuthUri(
            String email,
            String secret
    ) {
        if (email == null || email.isBlank()
                || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "Email and secret are required"
            );
        }

        String label = URLEncoder.encode(
                ISSUER + ":" + email.trim(),
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + ISSUER
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";
    }

    // Kiem tra ma OTP do nguoi dung nhap.
    public boolean verifyCode(
            String secret,
            String code
    ) {
        if (secret == null
                || code == null
                || !code.matches("[0-9]{6}")) {
            return false;
        }

        byte[] secretBytes;

        try {
            secretBytes = decodeBase32(secret);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        if (secretBytes.length < 10) {
            return false;
        }

        long currentCounter =
                Instant.now().getEpochSecond()
                        / TIME_STEP_SECONDS;

        // Chap nhan lech thoi gian toi da 30 giay.
        for (int offset = -1; offset <= 1; offset++) {
            String expected = calculateCode(
                    secretBytes,
                    currentCounter + offset
            );

            if (MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII)
            )) {
                return true;
            }
        }

        return false;
    }

    // Tinh ma TOTP theo HMAC-SHA1.
    private String calculateCode(
            byte[] secret,
            long counter
    ) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");

            hmac.init(
                    new SecretKeySpec(secret, "HmacSHA1")
            );

            byte[] counterBytes =
                    ByteBuffer.allocate(Long.BYTES)
                            .putLong(counter)
                            .array();

            byte[] hash = hmac.doFinal(counterBytes);

            int offset =
                    hash[hash.length - 1] & 0x0f;

            int binary =
                    ((hash[offset] & 0x7f) << 24)
                            | ((hash[offset + 1] & 0xff) << 16)
                            | ((hash[offset + 2] & 0xff) << 8)
                            | (hash[offset + 3] & 0xff);

            return String.format(
                    Locale.ROOT,
                    "%0" + DIGITS + "d",
                    binary % 1_000_000
            );
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "TOTP algorithm is unavailable",
                    ex
            );
        }
    }

    // Chuyen mang byte sang Base32.
    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();

        int buffer = 0;
        int bitCount = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitCount += 8;

            while (bitCount >= 5) {
                bitCount -= 5;

                result.append(
                        BASE32_ALPHABET.charAt(
                                (buffer >>> bitCount) & 31
                        )
                );
            }
        }

        if (bitCount > 0) {
            result.append(
                    BASE32_ALPHABET.charAt(
                            (buffer << (5 - bitCount)) & 31
                    )
            );
        }

        return result.toString();
    }

    // Giai ma Base32 thanh byte de xac thuc TOTP.
    private byte[] decodeBase32(String text) {
        String input = text.trim()
                .replace("=", "")
                .toUpperCase(Locale.ROOT);

        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        int buffer = 0;
        int bitCount = 0;

        for (int i = 0; i < input.length(); i++) {
            int value = BASE32_ALPHABET.indexOf(
                    input.charAt(i)
            );

            if (value == -1) {
                throw new IllegalArgumentException(
                        "Invalid Base32 secret"
                );
            }

            buffer = (buffer << 5) | value;
            bitCount += 5;

            if (bitCount >= 8) {
                bitCount -= 8;

                bytes.write(
                        (buffer >>> bitCount) & 0xff
                );
            }
        }

        return bytes.toByteArray();
    }
}
