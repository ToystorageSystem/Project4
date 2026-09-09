package com.toystorage.backend.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RolePermissionResponse {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private List<PermissionResponse> permissions;
}