package com.toystorage.backend.services.auth;

import com.toystorage.backend.config.security.CustomUserDetails;

import com.toystorage.backend.dto.response.auth.PermissionImpactResponse;
import com.toystorage.backend.dto.response.auth.PermissionResponse;
import com.toystorage.backend.dto.response.auth.RolePermissionResponse;
import com.toystorage.backend.dto.response.auth.RoleUserResponse;

import com.toystorage.backend.entity.auth.Permissions;
import com.toystorage.backend.entity.auth.RolePermissions;
import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.auth.PermissionStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.users.UserStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.auth.PermissionRepository;
import com.toystorage.backend.repository.auth.RolePermissionRepository;
import com.toystorage.backend.repository.auth.RoleRepository;
import com.toystorage.backend.repository.auth.UserRoleRepository;

import com.toystorage.backend.repository.users.UserRepository;

import com.toystorage.backend.services.users.ActivityLogService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RolePermissionManagementService {

    // =========================================================
    // REPOSITORIES / SERVICES
    // =========================================================

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final UserRoleRepository userRoleRepository;

    private final UserRepository userRepository;

    private final ActivityLogService activityLogService;


    // =========================================================
    // GRANT PERMISSION
    // =========================================================

    @Transactional
    public RolePermissionResponse grantPermission(
            Long roleId,
            Long permissionId
    ) {

        // -----------------------------------------------------
        // 1. ROLE
        // -----------------------------------------------------

        Roles role =
                getRoleEntity(
                        roleId
                );


        // -----------------------------------------------------
        // 2. PERMISSION
        // -----------------------------------------------------

        Permissions permission =
                getPermissionEntity(
                        permissionId
                );


        // Không gán Permission đang INACTIVE.
        if (
                permission.getStatus()
                        != PermissionStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Quyền đang không hoạt động"
            );
        }


        // -----------------------------------------------------
        // 3. DUPLICATE CHECK
        // -----------------------------------------------------

        boolean alreadyExists =
                rolePermissionRepository
                        .existsByRole_IdAndPermission_Id(
                                roleId,
                                permissionId
                        );


        if (alreadyExists) {

            throw new BadRequest(
                    "Vai trò đã có quyền này"
            );
        }


        // -----------------------------------------------------
        // 4. CURRENT ADMIN
        // -----------------------------------------------------

        Users actor =
                getCurrentActor();


        // -----------------------------------------------------
        // 5. CREATE MAPPING
        // -----------------------------------------------------

        RolePermissions mapping =
                new RolePermissions();


        mapping.setRole(
                role
        );


        mapping.setPermission(
                permission
        );


        mapping.setGrantedBy(
                actor
        );


        // grantedAt được @PrePersist tự set.
        rolePermissionRepository.save(
                mapping
        );


        // -----------------------------------------------------
        // 6. ACTIVITY LOG
        // -----------------------------------------------------

        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "roleId",
                role.getId()
        );


        newValue.put(
                "roleCode",
                role.getRoleCode()
        );


        newValue.put(
                "permissionId",
                permission.getId()
        );


        newValue.put(
                "permissionCode",
                permission.getPermissionCode()
        );


        logRolePermissionActivity(
                ActivityAction.GRANT_PERMISSION,
                role.getId(),
                null,
                newValue
        );


        return buildRolePermissionResponse(
                role
        );
    }


    // =========================================================
    // REVOKE PERMISSION
    // =========================================================

    @Transactional
    public RolePermissionResponse revokePermission(
            Long roleId,
            Long permissionId
    ) {

        // -----------------------------------------------------
        // 1. ROLE
        // -----------------------------------------------------

        Roles role =
                getRoleEntity(
                        roleId
                );


        // -----------------------------------------------------
        // 2. PERMISSION
        // -----------------------------------------------------

        Permissions permission =
                getPermissionEntity(
                        permissionId
                );


        // -----------------------------------------------------
        // 3. FIND MAPPING
        // -----------------------------------------------------

        RolePermissions mapping =
                rolePermissionRepository
                        .findByRole_IdAndPermission_Id(
                                roleId,
                                permissionId
                        )
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Vai trò chưa có quyền này"
                                )
                        );


        // -----------------------------------------------------
        // 4. PROTECT LAST ADMIN
        // -----------------------------------------------------

        /*
         * ROLE_MANAGE là quyền dùng để truy cập
         * chức năng quản lý Role / Permission.
         *
         * AdminRolePermissionController đang yêu cầu:
         *
         * ROLE_ADMIN + ROLE_MANAGE
         *
         * Vì vậy nếu chỉ còn 1 Admin ACTIVE,
         * không được thu hồi ROLE_MANAGE
         * khỏi role ADMIN.
         */

        boolean adminRole =
                "ADMIN".equalsIgnoreCase(
                        role.getRoleCode()
                );


        boolean roleManagePermission =
                "ROLE_MANAGE".equalsIgnoreCase(
                        permission.getPermissionCode()
                );


        if (
                adminRole
                        && roleManagePermission
        ) {

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
                        "Không thể thu hồi quyền ROLE_MANAGE khỏi Admin cuối cùng"
                );
            }
        }


        // -----------------------------------------------------
        // 5. OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "roleId",
                role.getId()
        );


        oldValue.put(
                "roleCode",
                role.getRoleCode()
        );


        oldValue.put(
                "permissionId",
                permission.getId()
        );


        oldValue.put(
                "permissionCode",
                permission.getPermissionCode()
        );


        // -----------------------------------------------------
        // 6. DELETE MAPPING
        // -----------------------------------------------------

        /*
         * Chỉ tới đây sau khi đã qua business rule.
         */
        rolePermissionRepository.delete(
                mapping
        );


        rolePermissionRepository.flush();


        // -----------------------------------------------------
        // 7. ACTIVITY LOG
        // -----------------------------------------------------

        logRolePermissionActivity(
                ActivityAction.REVOKE_PERMISSION,
                role.getId(),
                oldValue,
                null
        );


        // -----------------------------------------------------
        // 8. RESPONSE
        // -----------------------------------------------------

        return buildRolePermissionResponse(
                role
        );
    }


    // =========================================================
    // COPY PERMISSIONS FROM ROLE
    // =========================================================

    /*
     * sourceRole -> targetRole
     *
     * Quy tắc:
     *
     * - Source phải tồn tại
     * - Target phải tồn tại
     * - Source != Target
     * - Chỉ copy Permission ACTIVE
     * - Không duplicate
     * - Không xóa Permission cũ của Target
     * - Lưu grantedBy
     * - Có Activity Log
     */
    @Transactional
    public RolePermissionResponse copyPermissions(
            Long targetRoleId,
            Long sourceRoleId
    ) {

        // -----------------------------------------------------
        // 1. SOURCE ROLE
        // -----------------------------------------------------

        Roles sourceRole =
                getRoleEntity(
                        sourceRoleId
                );


        // -----------------------------------------------------
        // 2. TARGET ROLE
        // -----------------------------------------------------

        Roles targetRole =
                getRoleEntity(
                        targetRoleId
                );


        // -----------------------------------------------------
        // 3. SOURCE != TARGET
        // -----------------------------------------------------

        if (
                sourceRoleId.equals(
                        targetRoleId
                )
        ) {

            throw new BadRequest(
                    "Vai trò nguồn và vai trò đích không được giống nhau"
            );
        }


        // -----------------------------------------------------
        // 4. CURRENT ADMIN
        // -----------------------------------------------------

        Users actor =
                getCurrentActor();


        // -----------------------------------------------------
        // 5. SOURCE PERMISSIONS
        // -----------------------------------------------------

        List<RolePermissions> sourceMappings =
                rolePermissionRepository
                        .findByRole_IdOrderByPermission_IdAsc(
                                sourceRoleId
                        );


        // -----------------------------------------------------
        // 6. BUILD NEW MAPPINGS
        // -----------------------------------------------------

        List<RolePermissions> newMappings =
                new ArrayList<>();


        for (
                RolePermissions sourceMapping
                : sourceMappings
        ) {

            Permissions permission =
                    sourceMapping.getPermission();


            if (permission == null) {
                continue;
            }


            // Chỉ copy quyền ACTIVE.
            if (
                    permission.getStatus()
                            != PermissionStatus.ACTIVE
            ) {

                continue;
            }


            // Target đã có quyền thì bỏ qua.
            boolean alreadyExists =
                    rolePermissionRepository
                            .existsByRole_IdAndPermission_Id(
                                    targetRoleId,
                                    permission.getId()
                            );


            if (alreadyExists) {
                continue;
            }


            RolePermissions newMapping =
                    new RolePermissions();


            newMapping.setRole(
                    targetRole
            );


            newMapping.setPermission(
                    permission
            );


            newMapping.setGrantedBy(
                    actor
            );


            newMappings.add(
                    newMapping
            );
        }


        // -----------------------------------------------------
        // 7. SAVE
        // -----------------------------------------------------

        if (
                !newMappings.isEmpty()
        ) {

            rolePermissionRepository
                    .saveAll(
                            newMappings
                    );


            rolePermissionRepository.flush();
        }


        // -----------------------------------------------------
        // 8. ACTIVITY LOG
        // -----------------------------------------------------

        for (
                RolePermissions mapping
                : newMappings
        ) {

            Permissions permission =
                    mapping.getPermission();


            Map<String, Object> newValue =
                    new LinkedHashMap<>();


            newValue.put(
                    "sourceRoleId",
                    sourceRole.getId()
            );


            newValue.put(
                    "sourceRoleCode",
                    sourceRole.getRoleCode()
            );


            newValue.put(
                    "targetRoleId",
                    targetRole.getId()
            );


            newValue.put(
                    "targetRoleCode",
                    targetRole.getRoleCode()
            );


            newValue.put(
                    "permissionId",
                    permission.getId()
            );


            newValue.put(
                    "permissionCode",
                    permission.getPermissionCode()
            );


            newValue.put(
                    "copied",
                    true
            );


            logRolePermissionActivity(
                    ActivityAction.GRANT_PERMISSION,
                    targetRole.getId(),
                    null,
                    newValue
            );
        }


        // -----------------------------------------------------
        // 9. RESPONSE
        // -----------------------------------------------------

        return buildRolePermissionResponse(
                targetRole
        );
    }


    // =========================================================
    // CHECK PERMISSION IMPACT
    // =========================================================

    /*
     * Chỉ PREVIEW.
     *
     * KHÔNG:
     *
     * - DELETE mapping
     * - UPDATE Permission
     * - UPDATE Role
     * - expire session
     *
     * API này dùng để Admin xem trước
     * những User nào sẽ bị ảnh hưởng nếu revoke.
     */
    @Transactional(readOnly = true)
    public PermissionImpactResponse checkPermissionImpact(
            Long roleId,
            Long permissionId
    ) {

        // -----------------------------------------------------
        // 1. ROLE
        // -----------------------------------------------------

        Roles role =
                getRoleEntity(
                        roleId
                );


        // -----------------------------------------------------
        // 2. PERMISSION
        // -----------------------------------------------------

        Permissions permission =
                getPermissionEntity(
                        permissionId
                );


        // -----------------------------------------------------
        // 3. ROLE CÓ PERMISSION NÀY KHÔNG?
        // -----------------------------------------------------

        boolean assigned =
                rolePermissionRepository
                        .existsByRole_IdAndPermission_Id(
                                roleId,
                                permissionId
                        );


        // -----------------------------------------------------
        // 4. USERS BỊ ẢNH HƯỞNG
        // -----------------------------------------------------

        List<RoleUserResponse> affectedUsers;


        if (assigned) {

            affectedUsers =
                    userRoleRepository
                            .findByRole_Id(
                                    roleId
                            )
                            .stream()

                            .map(
                                    UserRoles::getUser
                            )

                            .filter(user ->
                                    user != null
                            )

                            .map(
                                    this::toRoleUserResponse
                            )

                            .toList();

        } else {

            affectedUsers =
                    Collections.emptyList();
        }


        // -----------------------------------------------------
        // 5. ADMIN INFORMATION
        // -----------------------------------------------------

        boolean adminRole =
                "ADMIN".equalsIgnoreCase(
                        role.getRoleCode()
                );


        boolean roleManagePermission =
                "ROLE_MANAGE".equalsIgnoreCase(
                        permission.getPermissionCode()
                );


        long activeAdminUserCount =
                userRoleRepository
                        .countUsersByRoleCodeAndStatus(
                                "ADMIN",
                                UserStatus.ACTIVE
                        );


        /*
         * Chỉ cảnh báo Admin cuối cùng khi:
         *
         * - mapping thực sự tồn tại
         * - role = ADMIN
         * - permission = ROLE_MANAGE
         * - chỉ còn <= 1 Admin ACTIVE
         */
        boolean lastAdminRisk =
                assigned
                        && adminRole
                        && roleManagePermission
                        && activeAdminUserCount <= 1;


        // -----------------------------------------------------
        // 6. RESPONSE
        // -----------------------------------------------------

        return PermissionImpactResponse
                .builder()

                .roleId(
                        role.getId()
                )

                .roleCode(
                        role.getRoleCode()
                )

                .roleName(
                        role.getRoleName()
                )

                .permissionId(
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

                .assigned(
                        assigned
                )

                .affectedUserCount(
                        affectedUsers.size()
                )

                .affectedUsers(
                        affectedUsers
                )

                .adminRole(
                        adminRole
                )

                .activeAdminUserCount(
                        activeAdminUserCount
                )

                .lastAdminRisk(
                        lastAdminRisk
                )

                .build();
    }


    // =========================================================
    // USERS AFFECTED BY ROLE
    // =========================================================

    /*
     * Dùng để expire session sau khi:
     *
     * - grant permission
     * - revoke permission
     * - copy permission
     * - lock role
     * - unlock role
     */
    @Transactional(readOnly = true)
    public List<Long> getUserIdsByRole(
            Long roleId
    ) {

        return userRoleRepository
                .findByRole_Id(
                        roleId
                )
                .stream()

                .map(
                        UserRoles::getUser
                )

                .filter(user ->
                        user != null
                )

                .map(
                        Users::getId
                )

                .distinct()

                .toList();
    }


    // =========================================================
    // GET ROLE
    // =========================================================

    private Roles getRoleEntity(
            Long roleId
    ) {

        return roleRepository
                .findById(
                        roleId
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy vai trò"
                        )
                );
    }


    // =========================================================
    // GET PERMISSION
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
    // CURRENT ADMIN
    // =========================================================

    private Users getCurrentActor() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            throw new Unauthorized(
                    "Bạn chưa đăng nhập"
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (
                !(principal instanceof CustomUserDetails userDetails)
        ) {

            throw new Unauthorized(
                    "Phiên đăng nhập không hợp lệ"
            );
        }


        return userRepository
                .findById(
                        userDetails.getId()
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy tài khoản thực hiện thao tác"
                        )
                );
    }


    // =========================================================
    // BUILD ROLE PERMISSION RESPONSE
    // =========================================================

    private RolePermissionResponse buildRolePermissionResponse(
            Roles role
    ) {

        List<PermissionResponse> permissions =
                rolePermissionRepository
                        .findByRole_IdOrderByPermission_IdAsc(
                                role.getId()
                        )
                        .stream()

                        .map(
                                RolePermissions::getPermission
                        )

                        .map(
                                this::toPermissionResponse
                        )

                        .toList();


        return RolePermissionResponse
                .builder()

                .roleId(
                        role.getId()
                )

                .roleCode(
                        role.getRoleCode()
                )

                .roleName(
                        role.getRoleName()
                )

                .permissions(
                        permissions
                )

                .build();
    }


    // =========================================================
    // PERMISSION -> RESPONSE
    // =========================================================

    private PermissionResponse toPermissionResponse(
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
    // USER -> ROLE USER RESPONSE
    // =========================================================

    private RoleUserResponse toRoleUserResponse(
            Users user
    ) {

        Long warehouseId =
                user.getWarehouse() != null
                        ? user.getWarehouse().getId()
                        : null;


        return RoleUserResponse
                .builder()

                .id(
                        user.getId()
                )

                .userCode(
                        user.getUserCode()
                )

                .name(
                        user.getName()
                )

                .email(
                        user.getEmail()
                )

                .phone(
                        user.getPhone()
                )

                .status(
                        user.getStatus()
                )

                .warehouseId(
                        warehouseId
                )

                .build();
    }


    // =========================================================
    // ACTIVITY LOG
    // =========================================================

    private void logRolePermissionActivity(
            ActivityAction action,
            Long roleId,
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
                ActivityEntityType.ROLE,
                roleId,
                oldValue,
                newValue
        );
    }


    // =========================================================
    // CURRENT HTTP REQUEST
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