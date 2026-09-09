package com.toystorage.backend.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRoleResponse {

    private Long userRoleId;

    private String userRolesCode;

    private Long roleId;

    private String roleCode;

    private String roleName;
}