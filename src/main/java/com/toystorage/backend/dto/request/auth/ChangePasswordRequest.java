
package com.toystorage.backend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    // Mat khau hien tai cua tai khoan.
    @NotBlank(message = "Current password is required.")
    private String oldPassword;

    // Mat khau moi phai co it nhat 8 ky tu.
    @NotBlank(message = "New password is required.")
    @Size(
            min = 6,
            message = "New password must contain at least 6 characters."
    )
    private String newPassword;
}
