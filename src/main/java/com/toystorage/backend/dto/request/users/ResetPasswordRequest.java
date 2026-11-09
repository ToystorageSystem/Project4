package com.toystorage.backend.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(regexp = "[0-9]{6,72}", message = "Mật khẩu phải gồm từ 6 đến 72 chữ số")
    private String newPassword;
}