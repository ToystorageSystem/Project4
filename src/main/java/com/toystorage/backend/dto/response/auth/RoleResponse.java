package com.toystorage.backend.dto.response.auth;

import com.toystorage.backend.enums.auth.RoleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private RoleStatus status;
}