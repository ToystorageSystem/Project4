package com.toystorage.backend.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "[0-9]{6,72}", message = "Mật khẩu phải gồm từ 6 đến 72 chữ số")
    private String password;

    private Long warehouseId;
}