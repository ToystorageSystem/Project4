package com.toystorage.backend.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePermissionRequest {

    @NotBlank(message = "Mã quyền không được để trống")
    @Size(max = 100, message = "Mã quyền không được vượt quá 100 ký tự")
    private String permissionCode;


    @NotBlank(message = "Module không được để trống")
    @Size(max = 80, message = "Module không được vượt quá 80 ký tự")
    private String moduleName;


    @NotBlank(message = "Action không được để trống")
    @Size(max = 50, message = "Action không được vượt quá 50 ký tự")
    private String actionName;


    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;
}