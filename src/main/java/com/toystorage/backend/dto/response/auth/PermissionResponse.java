package com.toystorage.backend.dto.response.auth;

import com.toystorage.backend.enums.auth.PermissionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {

    private Long id;

    private String permissionCode;

    private String moduleName;

    private String actionName;

    private String description;

    private PermissionStatus status;
}