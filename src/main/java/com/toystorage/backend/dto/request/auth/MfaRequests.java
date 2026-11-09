package com.toystorage.backend.dto.request.auth;
import jakarta.validation.constraints.*;

public final class MfaRequests {
    private MfaRequests() {}
    public record Otp(@NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}
    public record Recovery(@NotBlank @Size(max = 64) String code) {}
    public record ChangePassword(@NotBlank @Size(max = 72) String currentPassword,
                                 @NotBlank @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters") String newPassword) {}
}
