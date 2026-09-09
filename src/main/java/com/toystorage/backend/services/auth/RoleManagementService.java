package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.request.auth.CreateRoleRequest;
import com.toystorage.backend.dto.request.auth.UpdateRoleRequest;

import com.toystorage.backend.dto.response.auth.RoleResponse;

import com.toystorage.backend.entity.auth.Roles;

import com.toystorage.backend.enums.auth.RoleStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.auth.RolePermissionRepository;
import com.toystorage.backend.repository.auth.RoleRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    // =========================================================
    // REPOSITORIES / SERVICES
    // =========================================================

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final ActivityLogService activityLogService;


    // =========================================================
    // CREATE ROLE
    // =========================================================

    @Transactional
    public RoleResponse createRole(
            CreateRoleRequest request
    ) {

        String roleCode =
                request.getRoleCode()
                        .trim()
                        .toUpperCase();


        String roleName =
                request.getRoleName()
                        .trim();


        String description =
                normalizeDescription(
                        request.getDescription()
                );


        // -----------------------------------------------------
        // ROLE CODE UNIQUE
        // -----------------------------------------------------

        if (
                roleRepository
                        .existsByRoleCode(
                                roleCode
                        )
        ) {

            throw new BadRequest(
                    "Mã vai trò đã tồn tại"
            );
        }


        // -----------------------------------------------------
        // ROLE NAME UNIQUE
        // -----------------------------------------------------

        if (
                roleRepository
                        .existsByRoleNameIgnoreCase(
                                roleName
                        )
        ) {

            throw new BadRequest(
                    "Tên vai trò đã tồn tại"
            );
        }


        // -----------------------------------------------------
        // CREATE ENTITY
        // -----------------------------------------------------

        Roles role =
                new Roles();


        role.setRoleCode(
                roleCode
        );


        role.setRoleName(
                roleName
        );


        role.setDescription(
                description
        );


        role.setStatus(
                RoleStatus.ACTIVE
        );


        Roles savedRole =
                roleRepository.save(
                        role
                );


        // -----------------------------------------------------
        // ACTIVITY LOG
        // -----------------------------------------------------

        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "roleCode",
                savedRole.getRoleCode()
        );


        newValue.put(
                "roleName",
                savedRole.getRoleName()
        );


        newValue.put(
                "description",
                savedRole.getDescription()
        );


        newValue.put(
                "status",
                savedRole.getStatus()
        );


        logActivity(
                ActivityAction.CREATE,
                savedRole.getId(),
                null,
                newValue
        );


        return toResponse(
                savedRole
        );
    }


    // =========================================================
    // UPDATE ROLE
    // =========================================================

    /*
     * Cho phép sửa:
     *
     * - roleName
     * - description
     *
     * Không cho sửa roleCode
     * vì roleCode đang được Spring Security sử dụng.
     *
     * Không thay đổi status tại đây.
     * Khóa / mở khóa Role sử dụng API riêng.
     */
    @Transactional
    public RoleResponse updateRole(

            Long roleId,

            UpdateRoleRequest request
    ) {

        Roles role =
                getRoleEntity(
                        roleId
                );


        String newRoleName =
                request.getRoleName()
                        .trim();


        String newDescription =
                normalizeDescription(
                        request.getDescription()
                );


        // -----------------------------------------------------
        // ROLE NAME UNIQUE
        // -----------------------------------------------------

        if (
                roleRepository
                        .existsByRoleNameIgnoreCaseAndIdNot(
                                newRoleName,
                                roleId
                        )
        ) {

            throw new BadRequest(
                    "Tên vai trò đã tồn tại"
            );
        }


        // -----------------------------------------------------
        // OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "roleName",
                role.getRoleName()
        );


        oldValue.put(
                "description",
                role.getDescription()
        );


        // -----------------------------------------------------
        // UPDATE
        // -----------------------------------------------------

        role.setRoleName(
                newRoleName
        );


        role.setDescription(
                newDescription
        );


        Roles savedRole =
                roleRepository.save(
                        role
                );


        // -----------------------------------------------------
        // NEW VALUE
        // -----------------------------------------------------

        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "roleName",
                savedRole.getRoleName()
        );


        newValue.put(
                "description",
                savedRole.getDescription()
        );


        // -----------------------------------------------------
        // ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.UPDATE,
                savedRole.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedRole
        );
    }


    // =========================================================
    // LOCK ROLE
    // =========================================================

    /*
     * ACTIVE -> INACTIVE
     *
     * Không xóa Role khỏi DB.
     *
     * Controller sẽ chịu trách nhiệm expire
     * session của các User đang sử dụng Role này.
     */
    @Transactional
    public RoleResponse lockRole(
            Long roleId
    ) {

        Roles role =
                getRoleEntity(
                        roleId
                );


        // -----------------------------------------------------
        // KHÔNG CHO KHÓA ROLE ADMIN
        // -----------------------------------------------------

        /*
         * ADMIN là vai trò quản trị cốt lõi.
         *
         * Nếu khóa cả Role ADMIN thì tất cả User
         * mang Role này sẽ mất quyền quản trị.
         */
        if (
                "ADMIN".equalsIgnoreCase(
                        role.getRoleCode()
                )
        ) {

            throw new BadRequest(
                    "Không thể khóa vai trò ADMIN"
            );
        }


        // -----------------------------------------------------
        // ROLE ĐÃ INACTIVE
        // -----------------------------------------------------

        if (
                role.getStatus()
                        == RoleStatus.INACTIVE
        ) {

            throw new BadRequest(
                    "Vai trò đã bị khóa"
            );
        }


        // -----------------------------------------------------
        // OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "status",
                role.getStatus()
        );


        // -----------------------------------------------------
        // ACTIVE -> INACTIVE
        // -----------------------------------------------------

        role.setStatus(
                RoleStatus.INACTIVE
        );


        Roles savedRole =
                roleRepository.save(
                        role
                );


        // -----------------------------------------------------
        // NEW VALUE
        // -----------------------------------------------------

        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "status",
                savedRole.getStatus()
        );


        // -----------------------------------------------------
        // ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.UPDATE,
                savedRole.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedRole
        );
    }


    // =========================================================
    // UNLOCK ROLE
    // =========================================================

    /*
     * INACTIVE -> ACTIVE
     *
     * Controller sẽ expire session của User
     * để lần đăng nhập sau authorities được load lại.
     */
    @Transactional
    public RoleResponse unlockRole(
            Long roleId
    ) {

        Roles role =
                getRoleEntity(
                        roleId
                );


        // -----------------------------------------------------
        // ROLE ĐANG ACTIVE
        // -----------------------------------------------------

        if (
                role.getStatus()
                        == RoleStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Vai trò đang hoạt động"
            );
        }


        // -----------------------------------------------------
        // OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "status",
                role.getStatus()
        );


        // -----------------------------------------------------
        // INACTIVE -> ACTIVE
        // -----------------------------------------------------

        role.setStatus(
                RoleStatus.ACTIVE
        );


        Roles savedRole =
                roleRepository.save(
                        role
                );


        // -----------------------------------------------------
        // NEW VALUE
        // -----------------------------------------------------

        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "status",
                savedRole.getStatus()
        );


        // -----------------------------------------------------
        // ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.UPDATE,
                savedRole.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedRole
        );
    }


    // =========================================================
    // DELETE ROLE
    // =========================================================

    /*
     * Hiện tại chỉ cho xóa Role khi:
     *
     * - Role tồn tại
     * - Không có User nào đang sử dụng
     * - Không còn Permission nào
     *
     * Requirement bảo vệ System Role quan trọng
     * sẽ tiếp tục được bổ sung trong Task #24.
     */
    @Transactional
    public void deleteRole(
            Long roleId
    ) {

        // -----------------------------------------------------
        // 1. ROLE PHẢI TỒN TẠI
        // -----------------------------------------------------

        Roles role =
                getRoleEntity(
                        roleId
                );


        // -----------------------------------------------------
        // 2. KHÔNG XÓA ROLE ĐANG ĐƯỢC USER SỬ DỤNG
        // -----------------------------------------------------

        boolean assignedToUser =
                userRoleRepository
                        .existsByRole_Id(
                                roleId
                        );


        if (assignedToUser) {

            throw new BadRequest(
                    "Không thể xóa vai trò đang được gán cho người dùng"
            );
        }


        // -----------------------------------------------------
        // 3. KHÔNG XÓA ROLE ĐANG CÓ PERMISSION
        // -----------------------------------------------------

        boolean hasPermissions =
                rolePermissionRepository
                        .existsByRole_Id(
                                roleId
                        );


        if (hasPermissions) {

            throw new BadRequest(
                    "Không thể xóa vai trò đang có quyền"
            );
        }


        // -----------------------------------------------------
        // 4. SNAPSHOT TRƯỚC KHI DELETE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "roleCode",
                role.getRoleCode()
        );


        oldValue.put(
                "roleName",
                role.getRoleName()
        );


        oldValue.put(
                "description",
                role.getDescription()
        );


        oldValue.put(
                "status",
                role.getStatus()
        );


        // -----------------------------------------------------
        // 5. DELETE
        // -----------------------------------------------------

        roleRepository.delete(
                role
        );


        /*
         * Flush ngay để nếu DB có FK khác
         * đang tham chiếu Role thì lỗi xảy ra
         * ngay trong transaction hiện tại.
         */
        roleRepository.flush();


        // -----------------------------------------------------
        // 6. ACTIVITY LOG
        // -----------------------------------------------------

        logActivity(
                ActivityAction.DELETE,
                roleId,
                oldValue,
                null
        );
    }


    // =========================================================
    // GET ROLE ENTITY
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
    // ROLE -> RESPONSE
    // =========================================================

    private RoleResponse toResponse(
            Roles role
    ) {

        return RoleResponse
                .builder()

                .id(
                        role.getId()
                )

                .roleCode(
                        role.getRoleCode()
                )

                .roleName(
                        role.getRoleName()
                )

                .description(
                        role.getDescription()
                )

                .status(
                        role.getStatus()
                )

                .build();
    }


    // =========================================================
    // NORMALIZE DESCRIPTION
    // =========================================================

    private String normalizeDescription(
            String description
    ) {

        if (description == null) {
            return null;
        }


        String normalized =
                description.trim();


        if (normalized.isEmpty()) {
            return null;
        }


        return normalized;
    }


    // =========================================================
    // ACTIVITY LOG
    // =========================================================

    private void logActivity(

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