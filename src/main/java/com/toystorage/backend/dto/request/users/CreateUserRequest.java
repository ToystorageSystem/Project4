package com.toystorage.backend.dto.request.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @NotBlank(message = "CCCD không được để trống")
    @Size(
            min = 12,
            max = 12,
            message = "CCCD phải có 12 ký tự"
    )
    private String identityCard;

    @NotBlank(message = "Mật khẩu ban đầu không được để trống")
    @Size(
            min = 6,
            message = "Mật khẩu phải có ít nhất 6 ký tự"
    )
    private String password;

    @NotEmpty(message = "Người dùng phải có ít nhất một vai trò")
    private List<Long> roleIds;

    private Long warehouseId;
}