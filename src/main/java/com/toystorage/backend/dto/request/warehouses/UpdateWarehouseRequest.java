package com.toystorage.backend.dto.request.warehouses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWarehouseRequest {

    @NotBlank(
            message = "Tên địa điểm không được để trống"
    )
    @Size(
            max = 150,
            message = "Tên địa điểm không được vượt quá 150 ký tự"
    )
    private String name;


    @NotBlank(
            message = "Địa chỉ không được để trống"
    )
    @Size(
            max = 255,
            message = "Địa chỉ không được vượt quá 255 ký tự"
    )
    private String address;


    @NotBlank(
            message = "Số điện thoại không được để trống"
    )
    @Pattern(
            regexp = "^\\+?[0-9]{9,15}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;
}