package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.dto.request.auth.CreatePermissionRequest;
import com.toystorage.backend.dto.request.auth.CreateRoleRequest;
import com.toystorage.backend.dto.request.auth.UpdatePermissionRequest;
import com.toystorage.backend.dto.request.auth.UpdateRoleRequest;

import com.toystorage.backend.dto.response.auth.PermissionImpactResponse;
import com.toystorage.backend.dto.response.auth.PermissionResponse;
import com.toystorage.backend.dto.response.auth.RolePermissionResponse;
import com.toystorage.backend.dto.response.auth.RoleResponse;
import com.toystorage.backend.dto.response.auth.RoleUserResponse;
import com.toystorage.backend.dto.response.auth.UserRoleResponse;

import com.toystorage.backend.services.auth.PermissionManagementService;
import com.toystorage.backend.services.auth.RoleManagementService;
import com.toystorage.backend.services.auth.RolePermissionManagementService;
import com.toystorage.backend.services.auth.RolePermissionQueryService;

import com.toystorage.backend.services.users.UserSessionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize(
        "hasRole('ADMIN') and hasAuthority('ROLE_MANAGE')"
)
public class AdminRolePermissionController {

    // =========================================================
    // SERVICES
    // =========================================================

    private final RolePermissionQueryService rolePermissionQueryService;

    private final RolePermissionManagementService rolePermissionManagementService;

    private final RoleManagementService roleManagementService;

    private final PermissionManagementService permissionManagementService;

    private final UserSessionService userSessionService;


    // =========================================================
    // ROLE
    // =========================================================


    // =========================================================
    // GET / SEARCH ROLES
    // =========================================================

    /*
     * GET /api/admin/roles
     *
     * GET /api/admin/roles?search=manager
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getRoles(

            @RequestParam(required = false)
            String search
    ) {

        List<RoleResponse> roles =
                rolePermissionQueryService
                        .getRoles(
                                search
                        );

        return ResponseEntity.ok(
                roles
        );
    }


    // =========================================================
    // GET ROLE BY ID
    // =========================================================

    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(

            @PathVariable
            Long roleId
    ) {

        RoleResponse role =
                rolePermissionQueryService
                        .getRoleById(
                                roleId
                        );

        return ResponseEntity.ok(
                role
        );
    }


    // =========================================================
    // GET USERS OF ROLE
    // =========================================================

    @GetMapping("/roles/{roleId}/users")
    public ResponseEntity<List<RoleUserResponse>> getRoleUsers(

            @PathVariable
            Long roleId
    ) {

        List<RoleUserResponse> users =
                rolePermissionQueryService
                        .getRoleUsers(
                                roleId
                        );

        return ResponseEntity.ok(
                users
        );
    }


    // =========================================================
    // CREATE ROLE
    // =========================================================

    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(

            @Valid
            @RequestBody
            CreateRoleRequest request
    ) {

        RoleResponse role =
                roleManagementService
                        .createRole(
                                request
                        );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        role
                );
    }


    // =========================================================
    // UPDATE ROLE
    // =========================================================

    @PutMapping("/roles/{roleId}")
    public ResponseEntity<RoleResponse> updateRole(

            @PathVariable
            Long roleId,

            @Valid
            @RequestBody
            UpdateRoleRequest request
    ) {

        RoleResponse role =
                roleManagementService
                        .updateRole(
                                roleId,
                                request
                        );

        return ResponseEntity.ok(
                role
        );
    }


    // =========================================================
    // LOCK ROLE
    // =========================================================

    @PatchMapping("/roles/{roleId}/lock")
    public ResponseEntity<RoleResponse> lockRole(

            @PathVariable
            Long roleId
    ) {

        List<Long> affectedUserIds =
                rolePermissionManagementService
                        .getUserIdsByRole(
                                roleId
                        );

        RoleResponse role =
                roleManagementService
                        .lockRole(
                                roleId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                role
        );
    }


    // =========================================================
    // UNLOCK ROLE
    // =========================================================

    @PatchMapping("/roles/{roleId}/unlock")
    public ResponseEntity<RoleResponse> unlockRole(

            @PathVariable
            Long roleId
    ) {

        List<Long> affectedUserIds =
                rolePermissionManagementService
                        .getUserIdsByRole(
                                roleId
                        );

        RoleResponse role =
                roleManagementService
                        .unlockRole(
                                roleId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                role
        );
    }


    // =========================================================
    // DELETE ROLE
    // =========================================================

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(

            @PathVariable
            Long roleId
    ) {

        roleManagementService
                .deleteRole(
                        roleId
                );

        return ResponseEntity
                .noContent()
                .build();
    }


    // =========================================================
    // PERMISSION
    // =========================================================


    // =========================================================
    // GET ALL PERMISSIONS
    // =========================================================

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> getPermissions() {

        List<PermissionResponse> permissions =
                rolePermissionQueryService
                        .getPermissions();

        return ResponseEntity.ok(
                permissions
        );
    }


    // =========================================================
    // CREATE PERMISSION
    // =========================================================

    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponse> createPermission(

            @Valid
            @RequestBody
            CreatePermissionRequest request
    ) {

        PermissionResponse permission =
                permissionManagementService
                        .createPermission(
                                request
                        );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        permission
                );
    }


    // =========================================================
    // UPDATE PERMISSION
    // =========================================================

    @PutMapping("/permissions/{permissionId}")
    public ResponseEntity<PermissionResponse> updatePermission(

            @PathVariable
            Long permissionId,

            @Valid
            @RequestBody
            UpdatePermissionRequest request
    ) {

        PermissionResponse permission =
                permissionManagementService
                        .updatePermission(
                                permissionId,
                                request
                        );

        return ResponseEntity.ok(
                permission
        );
    }


    // =========================================================
    // INACTIVE PERMISSION
    // =========================================================

    @PatchMapping("/permissions/{permissionId}/inactive")
    public ResponseEntity<PermissionResponse> deactivatePermission(

            @PathVariable
            Long permissionId
    ) {

        List<Long> affectedUserIds =
                permissionManagementService
                        .getAffectedUserIds(
                                permissionId
                        );

        PermissionResponse permission =
                permissionManagementService
                        .deactivatePermission(
                                permissionId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                permission
        );
    }


    // =========================================================
    // ACTIVE PERMISSION
    // =========================================================

    @PatchMapping("/permissions/{permissionId}/active")
    public ResponseEntity<PermissionResponse> activatePermission(

            @PathVariable
            Long permissionId
    ) {

        List<Long> affectedUserIds =
                permissionManagementService
                        .getAffectedUserIds(
                                permissionId
                        );

        PermissionResponse permission =
                permissionManagementService
                        .activatePermission(
                                permissionId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                permission
        );
    }


    // =========================================================
    // ROLE - PERMISSION
    // =========================================================


    // =========================================================
    // GET PERMISSIONS OF ROLE
    // =========================================================

    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<RolePermissionResponse> getRolePermissions(

            @PathVariable
            Long roleId
    ) {

        RolePermissionResponse response =
                rolePermissionQueryService
                        .getRolePermissions(
                                roleId
                        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // COPY PERMISSIONS
    // =========================================================

    /*
     * POST
     *
     * /api/admin/roles/{targetRoleId}/copy-permissions/{sourceRoleId}
     */
    @PostMapping(
            "/roles/{targetRoleId}/copy-permissions/{sourceRoleId}"
    )
    public ResponseEntity<RolePermissionResponse> copyPermissions(

            @PathVariable
            Long targetRoleId,

            @PathVariable
            Long sourceRoleId
    ) {

        RolePermissionResponse response =
                rolePermissionManagementService
                        .copyPermissions(
                                targetRoleId,
                                sourceRoleId
                        );

        List<Long> affectedUserIds =
                rolePermissionManagementService
                        .getUserIdsByRole(
                                targetRoleId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // CHECK PERMISSION IMPACT
    // =========================================================

    /*
     * GET
     *
     * /api/admin/roles/{roleId}/permissions/{permissionId}/impact
     *
     * Preview only.
     * Không thay đổi DB.
     */
    @GetMapping(
            "/roles/{roleId}/permissions/{permissionId}/impact"
    )
    public ResponseEntity<PermissionImpactResponse> checkPermissionImpact(

            @PathVariable
            Long roleId,

            @PathVariable
            Long permissionId
    ) {

        PermissionImpactResponse response =
                rolePermissionManagementService
                        .checkPermissionImpact(
                                roleId,
                                permissionId
                        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // GRANT PERMISSION
    // =========================================================

    @PostMapping(
            "/roles/{roleId}/permissions/{permissionId}"
    )
    public ResponseEntity<RolePermissionResponse> grantPermission(

            @PathVariable
            Long roleId,

            @PathVariable
            Long permissionId
    ) {

        RolePermissionResponse response =
                rolePermissionManagementService
                        .grantPermission(
                                roleId,
                                permissionId
                        );

        List<Long> affectedUserIds =
                rolePermissionManagementService
                        .getUserIdsByRole(
                                roleId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // REVOKE PERMISSION
    // =========================================================

    @DeleteMapping(
            "/roles/{roleId}/permissions/{permissionId}"
    )
    public ResponseEntity<RolePermissionResponse> revokePermission(

            @PathVariable
            Long roleId,

            @PathVariable
            Long permissionId
    ) {

        RolePermissionResponse response =
                rolePermissionManagementService
                        .revokePermission(
                                roleId,
                                permissionId
                        );

        List<Long> affectedUserIds =
                rolePermissionManagementService
                        .getUserIdsByRole(
                                roleId
                        );

        expireAffectedUserSessions(
                affectedUserIds
        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // USER - ROLE
    // =========================================================


    // =========================================================
    // GET ROLES OF USER
    // =========================================================

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<UserRoleResponse>> getUserRoles(

            @PathVariable
            Long userId
    ) {

        List<UserRoleResponse> roles =
                rolePermissionQueryService
                        .getUserRoles(
                                userId
                        );

        return ResponseEntity.ok(
                roles
        );
    }


    // =========================================================
    // EXPIRE AFFECTED USER SESSIONS
    // =========================================================

    /*
     * Khi quyền / role thay đổi,
     * SecurityContext trong session hiện tại
     * vẫn chứa authorities cũ.
     *
     * Vì vậy phải expire session để user đăng nhập lại.
     */
    private void expireAffectedUserSessions(
            List<Long> userIds
    ) {

        if (
                userIds == null
                        || userIds.isEmpty()
        ) {

            return;
        }

        userIds
                .stream()

                .distinct()

                .forEach(userId ->

                        userSessionService
                                .expireSessionsByUserId(
                                        userId
                                )
                );
    }
}