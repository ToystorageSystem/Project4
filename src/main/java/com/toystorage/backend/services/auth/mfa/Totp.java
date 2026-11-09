package com.toystorage.backend.services.auth.mfa;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/** RFC 6238: SHA-1, 6 digits, 30 seconds; accepts ±1 time step. */
public final class Totp {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();
    private Totp() {}

    public static String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        StringBuilder result = new StringBuilder(32);
        int buffer = 0, bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 255);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                result.append(ALPHABET.charAt((buffer >>> bits) & 31));
            }
        }
        return result.toString();
    }

    public static String code(String secret, long step) {
        try {
            byte[] key = new byte[secret.length() * 5 / 8];
            int buffer = 0, bits = 0, index = 0;
            for (char c : secret.toCharArray()) {
                int value = ALPHABET.indexOf(c);
                if (value < 0) throw new IllegalArgumentException("Invalid Base32 secret");
                buffer = (buffer << 5) | value;
                bits += 5;
                if (bits >= 8) { bits -= 8; key[index++] = (byte)(buffer >>> bits); }
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 15;
            int number = ByteBuffer.wrap(hash, offset, 4).getInt() & 0x7fffffff;
            return String.format(java.util.Locale.ROOT, "%06d", number % 1_000_000);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("TOTP unavailable", e);
        }
    }

    public static long verify(String secret, String otp, long epochSeconds, long lastStep) {
        if (otp == null || !otp.matches("[0-9]{6}")) return -1;
        long now = epochSeconds / 30;
        for (long step = now - 1; step <= now + 1; step++) {
            if (step > lastStep && MessageDigest.isEqual(
                    code(secret, step).getBytes(StandardCharsets.US_ASCII),
                    otp.getBytes(StandardCharsets.US_ASCII))) return step;
        }
        return -1;
    }
}
