package com.toystorage.backend.dto.response.users;

import com.toystorage.backend.enums.users.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserResponse {

    private Long id;

    private String userCode;

    private String name;

    private String email;

    private String phone;

    private String identityCard;

    private UserStatus status;

    private Boolean mustChangePassword;

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private List<RoleInfo> roles;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    @Getter
    @Builder
    public static class RoleInfo {

        private Long id;

        private String roleCode;

        private String roleName;
    }
}