package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.request.auth.CreatePermissionRequest;
import com.toystorage.backend.dto.request.auth.UpdatePermissionRequest;

import com.toystorage.backend.dto.response.auth.PermissionResponse;

import com.toystorage.backend.entity.auth.Permissions;
import com.toystorage.backend.entity.auth.RolePermissions;
import com.toystorage.backend.entity.auth.UserRoles;

import com.toystorage.backend.enums.auth.PermissionStatus;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.users.UserStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.auth.PermissionRepository;
import com.toystorage.backend.repository.auth.RolePermissionRepository;
import com.toystorage.backend.repository.auth.UserRoleRepository;

import com.toystorage.backend.services.users.ActivityLogService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionManagementService {


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final PermissionRepository permissionRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final UserRoleRepository userRoleRepository;

    private final ActivityLogService activityLogService;


    // =========================================================
    // CREATE PERMISSION
    // =========================================================

    @Transactional
    public PermissionResponse createPermission(
            CreatePermissionRequest request
    ) {

        // -----------------------------------------------------
        // 1. NORMALIZE
        // -----------------------------------------------------

        String permissionCode =
                request.getPermissionCode()
                        .trim()
                        .toUpperCase();


        String moduleName =
                request.getModuleName()
                        .trim()
                        .toUpperCase();


        String actionName =
                request.getActionName()
                        .trim()
                        .toUpperCase();


        String description =
                normalizeNullable(
                        request.getDescription()
                );


        // -----------------------------------------------------
        // 2. UNIQUE PERMISSION CODE
        // -----------------------------------------------------

        if (
                permissionRepository
                        .existsByPermissionCodeIgnoreCase(
                                permissionCode
                        )
        ) {

            throw new BadRequest(
                    "Mã quyền đã tồn tại"
            );
        }


        // -----------------------------------------------------
        // 3. CREATE ENTITY
        // -----------------------------------------------------

        Permissions permission =
                Permissions.builder()

                        .permissionCode(
                                permissionCode
                        )

                        .moduleName(
                                moduleName
                        )

                        .actionName(
                                actionName
                        )

                        .description(
                                description
                        )

                        .status(
                                PermissionStatus.ACTIVE
                        )

                        .build();


        Permissions savedPermission =
                permissionRepository.save(
                        permission
                );


        // -----------------------------------------------------
        // 4. ACTIVITY LOG
        // -----------------------------------------------------

        Map<String, Object> newValue =
                snapshot(
                        savedPermission
                );


        logActivity(
                ActivityAction.CREATE,
                savedPermission.getId(),
                null,
                newValue
        );


        // -----------------------------------------------------
        // 5. RESPONSE
        // -----------------------------------------------------

        return toResponse(
                savedPermission
        );
    }


    // =========================================================
    // UPDATE PERMISSION
    // =========================================================

    /*
     * permissionCode KHÔNG được sửa.
     *
     * Chỉ sửa:
     *
     * - moduleName
     * - actionName
     * - description
     */
    @Transactional
    public PermissionResponse updatePermission(

            Long permissionId,

            UpdatePermissionRequest request
    ) {

        // -----------------------------------------------------
        // 1. GET PERMISSION
        // -----------------------------------------------------

        Permissions permission =
                getPermissionEntity(
                        permissionId
                );


        // -----------------------------------------------------
        // 2. OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                snapshot(
                        permission
                );


        // -----------------------------------------------------
        // 3. UPDATE
        // -----------------------------------------------------

        permission.setModuleName(

                request.getModuleName()
                        .trim()
                        .toUpperCase()
        );


        permission.setActionName(

                request.getActionName()
                        .trim()
                        .toUpperCase()
        );


        permission.setDescription(

                normalizeNullable(
                        request.getDescription()
                )
        );


        Permissions savedPermission =
                permissionRepository.save(
                        permission
                );


        // -----------------------------------------------------
        // 4. NEW VALUE
        // -----------------------------------------------------

        Map<String, Object> newValue =
                snapshot(
                        savedPermission
                );


        // -----------------------------------------------------
        // 5. ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.UPDATE,
                savedPermission.getId(),
                oldValue,
                newValue
        );


        // -----------------------------------------------------
        // 6. RESPONSE
        // -----------------------------------------------------

        return toResponse(
                savedPermission
        );
    }


    // =========================================================
    // INACTIVE PERMISSION
    // =========================================================

    @Transactional
    public PermissionResponse deactivatePermission(
            Long permissionId
    ) {

        // -----------------------------------------------------
        // 1. GET PERMISSION
        // -----------------------------------------------------

        Permissions permission =
                getPermissionEntity(
                        permissionId
                );


        // -----------------------------------------------------
        // 2. ALREADY INACTIVE
        // -----------------------------------------------------

        if (
                permission.getStatus()
                        == PermissionStatus.INACTIVE
        ) {

            throw new BadRequest(
                    "Quyền đã ở trạng thái INACTIVE"
            );
        }


        // -----------------------------------------------------
        // 3. PROTECT ROLE_MANAGE OF LAST ADMIN
        // -----------------------------------------------------

        /*
         * ROLE_MANAGE là quyền được dùng để truy cập
         * chức năng quản lý Role / Permission.
         *
         * AdminRolePermissionController đang yêu cầu:
         *
         * ROLE_ADMIN + ROLE_MANAGE
         *
         * Vì vậy nếu ROLE_MANAGE đang được gán cho ADMIN
         * và hệ thống chỉ còn 1 Admin ACTIVE,
         * tuyệt đối không được chuyển permission này
         * thành INACTIVE.
         */

        boolean roleManagePermission =
                "ROLE_MANAGE".equalsIgnoreCase(
                        permission.getPermissionCode()
                );


        if (roleManagePermission) {

            boolean assignedToAdmin =
                    rolePermissionRepository
                            .findByPermission_Id(
                                    permissionId
                            )

                            .stream()

                            .anyMatch(rolePermission ->

                                    rolePermission.getRole() != null

                                            && "ADMIN".equalsIgnoreCase(
                                                    rolePermission
                                                            .getRole()
                                                            .getRoleCode()
                                            )
                            );


            if (assignedToAdmin) {

                long activeAdminCount =
                        userRoleRepository
                                .countUsersByRoleCodeAndStatus(
                                        "ADMIN",
                                        UserStatus.ACTIVE
                                );


                if (
                        activeAdminCount <= 1
                ) {

                    throw new BadRequest(
                            "Không thể vô hiệu hóa quyền ROLE_MANAGE khi chỉ còn Admin cuối cùng"
                    );
                }
            }
        }


        // -----------------------------------------------------
        // 4. OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                snapshot(
                        permission
                );


        // -----------------------------------------------------
        // 5. UPDATE STATUS
        // -----------------------------------------------------

        permission.setStatus(
                PermissionStatus.INACTIVE
        );


        Permissions savedPermission =
                permissionRepository.save(
                        permission
                );


        // -----------------------------------------------------
        // 6. NEW VALUE
        // -----------------------------------------------------

        Map<String, Object> newValue =
                snapshot(
                        savedPermission
                );


        // -----------------------------------------------------
        // 7. ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.UPDATE,
                savedPermission.getId(),
                oldValue,
                newValue
        );


        // -----------------------------------------------------
        // 8. RESPONSE
        // -----------------------------------------------------

        return toResponse(
                savedPermission
        );
    }


    // =========================================================
    // ACTIVE PERMISSION
    // =========================================================

    @Transactional
    public PermissionResponse activatePermission(
            Long permissionId
    ) {

        // -----------------------------------------------------
        // 1. GET PERMISSION
        // -----------------------------------------------------

        Permissions permission =
                getPermissionEntity(
                        permissionId
                );


        // -----------------------------------------------------
        // 2. ALREADY ACTIVE
        // -----------------------------------------------------

        if (
                permission.getStatus()
                        == PermissionStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Quyền đã ở trạng thái ACTIVE"
            );
        }


        // -----------------------------------------------------
        // 3. OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                snapshot(
                        permission
                );


        // -----------------------------------------------------
        // 4. UPDATE STATUS
        // -----------------------------------------------------

        permission.setStatus(
                PermissionStatus.ACTIVE
        );


        Permissions savedPermission =
                permissionRepository.save(
                        permission
                );


        // -----------------------------------------------------
        // 5. NEW VALUE
        // -----------------------------------------------------

        Map<String, Object> newValue =
                snapshot(
                        savedPermission
                );


        // -----------------------------------------------------
        // 6. ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.UPDATE,
                savedPermission.getId(),
                oldValue,
                newValue
        );


        // -----------------------------------------------------
        // 7. RESPONSE
        // -----------------------------------------------------

        return toResponse(
                savedPermission
        );
    }


    // =========================================================
    // GET AFFECTED USER IDS
    // =========================================================

    /*
     * Permission
     *
     *      ↓
     *
     * role_permissions
     *
     *      ↓
     *
     * Role
     *
     *      ↓
     *
     * user_roles
     *
     *      ↓
     *
     * Users
     *
     * Khi Permission thay đổi trạng thái,
     * session của những User bị ảnh hưởng
     * phải được tạo lại để Security load
     * authorities mới.
     */
    @Transactional(readOnly = true)
    public List<Long> getAffectedUserIds(
            Long permissionId
    ) {

        // -----------------------------------------------------
        // 1. CHECK PERMISSION EXISTS
        // -----------------------------------------------------

        getPermissionEntity(
                permissionId
        );


        // -----------------------------------------------------
        // 2. FIND ROLES USING PERMISSION
        // -----------------------------------------------------

        List<RolePermissions> rolePermissions =
                rolePermissionRepository
                        .findByPermission_Id(
                                permissionId
                        );


        // -----------------------------------------------------
        // 3. FIND USERS OF THOSE ROLES
        // -----------------------------------------------------

        return rolePermissions
                .stream()

                .flatMap(rolePermission -> {

                    Long roleId =
                            rolePermission
                                    .getRole()
                                    .getId();


                    List<UserRoles> userRoles =
                            userRoleRepository
                                    .findByRole_Id(
                                            roleId
                                    );


                    return userRoles.stream();
                })

                .map(userRole ->
                        userRole
                                .getUser()
                                .getId()
                )

                .distinct()

                .toList();
    }


    // =========================================================
    // GET PERMISSION ENTITY
    // =========================================================

    private Permissions getPermissionEntity(
            Long permissionId
    ) {

        return permissionRepository
                .findById(
                        permissionId
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy quyền"
                        )
                );
    }


    // =========================================================
    // RESPONSE
    // =========================================================

    private PermissionResponse toResponse(
            Permissions permission
    ) {

        return PermissionResponse
                .builder()

                .id(
                        permission.getId()
                )

                .permissionCode(
                        permission.getPermissionCode()
                )

                .moduleName(
                        permission.getModuleName()
                )

                .actionName(
                        permission.getActionName()
                )

                .description(
                        permission.getDescription()
                )

                .status(
                        permission.getStatus()
                )

                .build();
    }


    // =========================================================
    // SNAPSHOT
    // =========================================================

    private Map<String, Object> snapshot(
            Permissions permission
    ) {

        Map<String, Object> value =
                new LinkedHashMap<>();


        value.put(
                "permissionCode",
                permission.getPermissionCode()
        );


        value.put(
                "moduleName",
                permission.getModuleName()
        );


        value.put(
                "actionName",
                permission.getActionName()
        );


        value.put(
                "description",
                permission.getDescription()
        );


        value.put(
                "status",
                permission.getStatus()
        );


        return value;
    }


    // =========================================================
    // NORMALIZE NULLABLE
    // =========================================================

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String trimmed =
                value.trim();


        return trimmed.isEmpty()
                ? null
                : trimmed;
    }


    // =========================================================
    // ACTIVITY LOG
    // =========================================================

    private void logActivity(

            ActivityAction action,

            Long permissionId,

            Object oldValue,

            Object newValue
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        HttpServletRequest request =
                getCurrentHttpRequest();


        activityLogService.log(

                authentication,

                request,

                action,

                ActivityEntityType.PERMISSION,

                permissionId,

                oldValue,

                newValue
        );
    }


    // =========================================================
    // GET CURRENT REQUEST
    // =========================================================

    private HttpServletRequest getCurrentHttpRequest() {

        if (
                RequestContextHolder
                        .getRequestAttributes()
                        instanceof ServletRequestAttributes attributes
        ) {

            return attributes.getRequest();
        }


        return null;
    }
}