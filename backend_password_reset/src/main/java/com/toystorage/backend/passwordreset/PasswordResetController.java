package com.toystorage.backend.passwordreset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.password-reset.enabled", havingValue = "true")
public class PasswordResetController {
    private final PasswordResetService service;
    public PasswordResetController(PasswordResetService service) { this.service = service; }
    public record Request(@NotBlank @Email @Size(max=150) String email) {}
    public record Reset(@NotBlank @Email @Size(max=150) String email,
            @NotBlank @Size(min=43,max=43) String token,
            @NotBlank @Size(min=12,max=64) String newPassword) {}
    @PostMapping("/forgot-password")
    public Map<String,String> request(@Valid @RequestBody Request body) {
        try { service.request(body.email().trim()); }
        catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dich vu email tam thoi khong san sang.");
        }
        return Map.of("message", "Neu tai khoan hop le, ma khoi phuc se duoc gui den email.");
    }
    @PostMapping("/reset-password")
    public Map<String,String> reset(@Valid @RequestBody Reset body) {
        service.reset(body.email().trim(), body.token().trim(), body.newPassword());
        return Map.of("message", "Mat khau da duoc cap nhat.");
    }
}
