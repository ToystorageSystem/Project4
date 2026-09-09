package com.toystorage.backend.dto.request.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(
            max = 150,
            message = "Họ tên tối đa 150 ký tự"
    )
    private String name;


    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(
            max = 150,
            message = "Email tối đa 150 ký tự"
    )
    private String email;


    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(
            max = 20,
            message = "Số điện thoại tối đa 20 ký tự"
    )
    private String phone;


    @NotBlank(message = "CCCD không được để trống")
    @Size(
            min = 12,
            max = 12,
            message = "CCCD phải có 12 ký tự"
    )
    private String identityCard;


    /*
     * Địa điểm làm việc.
     *
     * Có thể là:
     * - Kho tổng
     * - Cửa hàng
     * - null nếu nghiệp vụ cho phép
     *
     * Hiện tại KHÔNG cập nhật role.
     */
    private Long warehouseId;
}