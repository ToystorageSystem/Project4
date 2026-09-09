package com.toystorage.backend.dto.response.auth;

import com.toystorage.backend.enums.users.UserStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleUserResponse {

    private Long id;

    private String userCode;

    private String name;

    private String email;

    private String phone;

    private UserStatus status;

    private Long warehouseId;
}