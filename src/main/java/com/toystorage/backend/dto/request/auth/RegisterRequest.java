package com.toystorage.backend.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String userCode;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String identityCard;

    @NotBlank
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    private Long warehouseId;
}