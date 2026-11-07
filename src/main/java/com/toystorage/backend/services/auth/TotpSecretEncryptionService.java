
package com.toystorage.backend.services.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class TotpSecretEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final byte FORMAT_VERSION = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    private final SecretKeySpec encryptionKey;

    public TotpSecretEncryptionService(
            @Value("${TOYSTORAGE_TOTP_ENCRYPTION_KEY:}")
            String base64Key
    ) {
        // Chua co key thi khong cho phep ma hoa hoac giai ma.
        // Cho phep ung dung khoi dong trong giai doan phat trien.
        if (base64Key == null || base64Key.isBlank()) {
            this.encryptionKey = null;
            return;
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid TOTP encryption key format",
                    ex
            );
        }

        if (keyBytes.length != KEY_LENGTH) {
            throw new IllegalStateException(
                    "TOTP encryption key must be 32 bytes"
            );
        }

        this.encryptionKey = new SecretKeySpec(
                keyBytes,
                "AES"
        );

        Arrays.fill(keyBytes, (byte) 0);
    }

    /*
     * Ma hoa TOTP secret truoc khi luu database.
     *
     * Ket qua:
     * [version 1 byte][IV 12 bytes][ciphertext + authentication tag]
     *
     * userId duoc gan vao du lieu xac thuc AES-GCM,
     * ngan su dung nham secret cua tai khoan khac.
     */
    public byte[] encrypt(Long userId, String secret) {
        validateUserId(userId);

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "TOTP secret is required"
            );
        }

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    requireEncryptionKey(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(userIdBytes(userId));

            byte[] encrypted = cipher.doFinal(
                    secret.getBytes(StandardCharsets.UTF_8)
            );

            byte[] result = ByteBuffer.allocate(
                            1 + IV_LENGTH + encrypted.length
                    )
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(encrypted)
                    .array();

            if (result.length > 512) {
                throw new IllegalStateException(
                        "Encrypted TOTP secret exceeds storage size"
                );
            }

            return result;

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Unable to encrypt TOTP secret",
                    ex
            );
        }
    }

    /*
     * Giai ma secret khi can kiem tra ma OTP.
     *
     * Du lieu bi thay doi, sai key hoac sai userId
     * se khong the giai ma thanh cong.
     */
    public String decrypt(Long userId, byte[] encryptedData) {
        validateUserId(userId);

        int minLength = 1 + IV_LENGTH + 16;

        if (encryptedData == null
                || encryptedData.length < minLength
                || encryptedData[0] != FORMAT_VERSION) {

            throw new IllegalArgumentException(
                    "Invalid encrypted TOTP secret"
            );
        }

        byte[] iv = Arrays.copyOfRange(
                encryptedData,
                1,
                1 + IV_LENGTH
        );

        byte[] ciphertext = Arrays.copyOfRange(
                encryptedData,
                1 + IV_LENGTH,
                encryptedData.length
        );

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    requireEncryptionKey(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(userIdBytes(userId));

            byte[] decrypted = cipher.doFinal(ciphertext);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Unable to decrypt TOTP secret",
                    ex
            );
        }
    }

    // Khong cho phep ma hoa khi chua cau hinh key.
    private SecretKeySpec requireEncryptionKey() {
        if (encryptionKey == null) {
            throw new IllegalStateException(
                    "TOYSTORAGE_TOTP_ENCRYPTION_KEY is not configured"
            );
        }

        return encryptionKey;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Valid user ID is required"
            );
        }
    }

    private byte[] userIdBytes(Long userId) {
        return ByteBuffer.allocate(Long.BYTES)
                .putLong(userId)
                .array();
    }
}
