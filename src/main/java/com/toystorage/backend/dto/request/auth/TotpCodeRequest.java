
package com.toystorage.backend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TotpCodeRequest {

    // Ma OTP gom dung 6 chu so tu Google Authenticator.
    @NotBlank(message = "Vui lòng nhập mã xác thực")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "Mã xác thực phải gồm đúng 6 chữ số"
    )
    private String code;
}
