package com.toystorage.backend.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PermissionImpactResponse {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private Long permissionId;

    private String permissionCode;

    private String moduleName;

    private String actionName;

    /*
     * Role hiện có Permission này hay không.
     */
    private boolean assigned;

    /*
     * Số User thực sự bị ảnh hưởng nếu revoke.
     */
    private long affectedUserCount;

    /*
     * Danh sách User bị ảnh hưởng.
     */
    private List<RoleUserResponse> affectedUsers;

    /*
     * Role đang kiểm tra có phải ADMIN hay không.
     */
    private boolean adminRole;

    /*
     * Số tài khoản ACTIVE đang có Role ADMIN.
     */
    private long activeAdminUserCount;

    /*
     * Cảnh báo khi thao tác đang tác động tới
     * Role ADMIN trong lúc chỉ còn <= 1 Admin ACTIVE.
     *
     * Đây là warning preview.
     * Việc chặn revoke sẽ xử lý ở business rule.
     */
    private boolean lastAdminRisk;
}