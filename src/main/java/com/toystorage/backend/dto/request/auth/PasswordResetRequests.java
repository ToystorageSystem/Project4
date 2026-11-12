package com.toystorage.backend.dto.request.auth;
import jakarta.validation.constraints.*;
public final class PasswordResetRequests {
    private PasswordResetRequests() {}
    public record Forgot(@NotBlank @Email @Size(max = 150) String email) {}
    public record Reset(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters") String newPassword,
            @Pattern(regexp = "[0-9]{6}") String otp,
            @Size(max = 64) String recoveryCode) {}
}
