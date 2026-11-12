package com.toystorage.backend.services.auth.mfa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class MfaCrypto {
    private final SecretKeySpec key;
    public MfaCrypto(@Value("${app.mfa.encryption-key}") String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length != 32) throw new IllegalArgumentException("MFA key must contain 32 bytes");
        key = new SecretKeySpec(bytes, "AES");
    }
    public String encrypt(Long userId, String secret) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(userId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] data = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + data.length)
                    .put(iv).put(data).array());
        } catch (Exception e) { throw new IllegalStateException("Cannot encrypt MFA secret", e); }
    }
    public String decrypt(Long userId, String encrypted) {
        try {
            if (!encrypted.startsWith("v1:")) throw new IllegalArgumentException("Unsupported MFA encryption version");
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encrypted.substring(3)));
            byte[] iv = new byte[12]; buffer.get(iv);
            byte[] data = new byte[buffer.remaining()]; buffer.get(data);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(userId.toString().getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Cannot decrypt MFA secret", e); }
    }
}
