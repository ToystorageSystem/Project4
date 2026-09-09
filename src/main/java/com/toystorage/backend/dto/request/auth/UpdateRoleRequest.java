package com.toystorage.backend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {

    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 80, message = "Tên vai trò tối đa 80 ký tự")
    private String roleName;

    @Size(max = 255, message = "Mô tả vai trò tối đa 255 ký tự")
    private String description;
}