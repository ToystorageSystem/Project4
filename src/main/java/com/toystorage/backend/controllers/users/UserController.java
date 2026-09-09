package com.toystorage.backend.controllers.users;

import com.toystorage.backend.dto.request.users.CreateUserRequest;
import com.toystorage.backend.dto.request.users.ResetPasswordRequest;
import com.toystorage.backend.dto.request.users.UpdateUserRequest;
import com.toystorage.backend.dto.request.users.UpdateUserRolesRequest;

import com.toystorage.backend.dto.response.auth.UserRoleResponse;
import com.toystorage.backend.dto.response.users.ActivityLogResponse;
import com.toystorage.backend.dto.response.users.UserResponse;

import com.toystorage.backend.enums.users.UserStatus;

import com.toystorage.backend.services.users.ActivityLogService;
import com.toystorage.backend.services.users.UserManagementService;
import com.toystorage.backend.services.users.UserSessionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;

    private final UserSessionService userSessionService;

    private final ActivityLogService activityLogService;


    // =========================================================
    // GET USERS
    // =========================================================

    @GetMapping
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_READ')"
    )
    public ResponseEntity<Page<UserResponse>> getUsers(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long roleId,

            @RequestParam(required = false)
            Long warehouseId,

            @RequestParam(required = false)
            UserStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Pageable pageable =
                PageRequest.of(

                        page,

                        size,

                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );


        Page<UserResponse> users =
                userManagementService
                        .getUsers(
                                keyword,
                                roleId,
                                warehouseId,
                                status,
                                pageable
                        );


        return ResponseEntity.ok(
                users
        );
    }


    // =========================================================
    // GET USER BY ID
    // =========================================================

    @GetMapping("/{userId}")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_READ')"
    )
    public ResponseEntity<UserResponse> getUserById(

            @PathVariable
            Long userId
    ) {

        UserResponse user =
                userManagementService
                        .getUserById(
                                userId
                        );


        return ResponseEntity.ok(
                user
        );
    }


    // =========================================================
    // GET USER ACTIVITY HISTORY
    // =========================================================

    @GetMapping("/{userId}/activity-logs")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_READ')"
    )
    public ResponseEntity<List<ActivityLogResponse>>
    getUserActivityLogs(

            @PathVariable
            Long userId
    ) {

        List<ActivityLogResponse> logs =
                activityLogService
                        .getUserHistory(
                                userId
                        );


        return ResponseEntity.ok(
                logs
        );
    }


    // =========================================================
    // CREATE USER
    // =========================================================

    @PostMapping
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<UserResponse> createUser(

            @Valid
            @RequestBody
            CreateUserRequest request
    ) {

        UserResponse user =
                userManagementService
                        .createUser(
                                request
                        );


        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        user
                );
    }


    // =========================================================
    // UPDATE USER BASIC INFORMATION
    // =========================================================

    /*
     * Chỉ sửa:
     *
     * - name
     * - email
     * - phone
     * - identityCard
     * - warehouse
     *
     * KHÔNG thay đổi Role.
     */
    @PutMapping("/{userId}")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<UserResponse> updateUser(

            @PathVariable
            Long userId,

            @Valid
            @RequestBody
            UpdateUserRequest request
    ) {

        UserResponse user =
                userManagementService
                        .updateUser(
                                userId,
                                request
                        );


        return ResponseEntity.ok(
                user
        );
    }


    // =========================================================
    // UPDATE USER ROLES
    // =========================================================

    /*
     * Ví dụ:
     *
     * PUT /api/admin/users/18/roles
     *
     * {
     *   "roleIds": [8, 6]
     * }
     *
     * Sau khi Role thay đổi:
     *
     * session cũ phải bị revoke
     * vì authorities trong SecurityContext
     * vẫn là authorities cũ.
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<List<UserRoleResponse>>
    updateUserRoles(

            @PathVariable
            Long userId,

            @Valid
            @RequestBody
            UpdateUserRolesRequest request
    ) {

        // -----------------------------------------------------
        // 1. UPDATE DB + ACTIVITY LOG
        // -----------------------------------------------------

        List<UserRoleResponse> roles =
                userManagementService
                        .updateUserRoles(
                                userId,
                                request
                        );


        // -----------------------------------------------------
        // 2. REVOKE OLD SESSION
        // -----------------------------------------------------

        userSessionService
                .expireSessionsByUserId(
                        userId
                );


        // -----------------------------------------------------
        // 3. RESPONSE
        // -----------------------------------------------------

        return ResponseEntity.ok(
                roles
        );
    }


    // =========================================================
    // LOCK USER
    // =========================================================

    @PatchMapping("/{userId}/lock")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<UserResponse> lockUser(

            @PathVariable
            Long userId
    ) {

        UserResponse user =
                userManagementService
                        .lockUser(
                                userId
                        );


        /*
         * Tài khoản đã LOCK
         * thì session hiện tại phải bị thu hồi.
         */
        userSessionService
                .expireSessionsByUserId(
                        userId
                );


        return ResponseEntity.ok(
                user
        );
    }


    // =========================================================
    // UNLOCK USER
    // =========================================================

    @PatchMapping("/{userId}/unlock")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<UserResponse> unlockUser(

            @PathVariable
            Long userId
    ) {

        /*
         * LOCKED -> ACTIVE
         *
         * Không tạo lại session.
         *
         * User phải login lại.
         */
        UserResponse user =
                userManagementService
                        .unlockUser(
                                userId
                        );


        return ResponseEntity.ok(
                user
        );
    }


    // =========================================================
    // INACTIVE USER
    // =========================================================

    @PatchMapping("/{userId}/inactive")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<UserResponse> deactivateUser(

            @PathVariable
            Long userId
    ) {

        UserResponse user =
                userManagementService
                        .deactivateUser(
                                userId
                        );


        /*
         * INACTIVE không được tiếp tục
         * dùng session hiện tại.
         */
        userSessionService
                .expireSessionsByUserId(
                        userId
                );


        return ResponseEntity.ok(
                user
        );
    }


    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @PatchMapping("/{userId}/reset-password")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('USER_MANAGE')"
    )
    public ResponseEntity<Void> resetPassword(

            @PathVariable
            Long userId,

            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {

        userManagementService
                .resetPassword(
                        userId,
                        request
                );


        return ResponseEntity
                .noContent()
                .build();
    }
}