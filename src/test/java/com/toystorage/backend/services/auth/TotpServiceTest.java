
package com.toystorage.backend.services.auth;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private final TotpService totpService =
            new TotpService();

    @Test
    void shouldGenerateValidSecret() {
        String secret = totpService.generateSecret();

        assertNotNull(secret);
        assertEquals(32, secret.length());
        assertTrue(secret.matches("[A-Z2-7]+"));
    }

    @Test
    void shouldAcceptGoogleAuthenticatorCode() {
        String secret = totpService.generateSecret();

        GoogleAuthenticator authenticator =
                new GoogleAuthenticator();

        int generatedCode =
                authenticator.getTotpPassword(secret);

        String code = String.format(
                Locale.ROOT,
                "%06d",
                generatedCode
        );

        assertTrue(totpService.verifyCode(secret, code));
    }

    @Test
    void shouldRejectInvalidCodeFormat() {
        String secret = totpService.generateSecret();

        assertFalse(totpService.verifyCode(secret, ""));
        assertFalse(totpService.verifyCode(secret, "12345"));
        assertFalse(totpService.verifyCode(secret, "abcdef"));
        assertFalse(totpService.verifyCode(secret, "1234567"));
    }

    @Test
    void shouldCreateOtpAuthUri() {
        String secret = totpService.generateSecret();

        String uri = totpService.createOtpAuthUri(
                "test@example.com",
                secret
        );

        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }
}
