package com.toystorage.backend.passwordreset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class PasswordResetServiceTest {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final JavaMailSender mail = mock(JavaMailSender.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    final PasswordResetService service = new PasswordResetService(jdbc, mail, encoder, "noreply@example.com");
    @Test void nonexistentAccountDoesNotSendEmail() {
        when(jdbc.queryForList(anyString(), eq("missing@example.com"))).thenReturn(List.of());
        service.request("missing@example.com"); verifyNoInteractions(mail);
    }
    @Test void expiredTokenNeverChangesPassword() {
        when(jdbc.queryForList(anyString(), eq("staff@example.com"))).thenReturn(List.of(Map.of(
                "user_id", 1L, "token_hash", PasswordResetService.hash("token"), "expires_at", Timestamp.from(Instant.now().minusSeconds(1)))));
        assertThrows(ResponseStatusException.class, () -> service.reset("staff@example.com", "token", "A-new-password-123"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void validTokenHashesPasswordAndDeletesRecoveryToken() {
        when(jdbc.queryForList(anyString(), eq("staff@example.com"))).thenReturn(List.of(Map.of(
                "user_id", 1L, "token_hash", PasswordResetService.hash("token"), "expires_at", Timestamp.from(Instant.now().plusSeconds(900)))));
        service.reset("staff@example.com", "token", "A-new-password-123");
        ArgumentCaptor<String> encoded = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(startsWith("UPDATE users"), encoded.capture(), any(Timestamp.class), eq(1L));
        assertTrue(encoder.matches("A-new-password-123", encoded.getValue()));
        verify(jdbc).update("DELETE FROM password_reset_tokens WHERE user_id=?", 1L);
    }
    @Test void resendCooldownDoesNotReplaceTokenOrSendMail() {
        when(jdbc.queryForList(anyString(), eq("staff@example.com"))).thenReturn(List.of(Map.of("id", 1L, "email", "staff@example.com")));
        when(jdbc.queryForList(anyString(), eq(1L))).thenReturn(List.of(Map.of("issued_at", Timestamp.from(Instant.now()))));
        service.request("staff@example.com"); verify(mail, never()).send(any(SimpleMailMessage.class));
    }
}
