package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.response.auth.PermissionResponse;
import com.toystorage.backend.dto.response.auth.RolePermissionResponse;
import com.toystorage.backend.dto.response.auth.RoleResponse;
import com.toystorage.backend.dto.response.auth.RoleUserResponse;
import com.toystorage.backend.dto.response.auth.UserRoleResponse;

import com.toystorage.backend.entity.auth.Permissions;
import com.toystorage.backend.entity.auth.RolePermissions;
import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.auth.UserRoles;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.auth.PermissionRepository;
import com.toystorage.backend.repository.auth.RolePermissionRepository;
import com.toystorage.backend.repository.auth.RoleRepository;
import com.toystorage.backend.repository.auth.UserRoleRepository;

import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionQueryService {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final UserRoleRepository userRoleRepository;

    private final UserRepository userRepository;


    // =========================================================
    // GET ALL ROLES
    // =========================================================

    /*
     * Giữ method cũ để tránh ảnh hưởng code khác.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {

        return getRoles(
                null
        );
    }


    // =========================================================
    // GET / SEARCH ROLES
    // =========================================================

    /*
     * GET /api/admin/roles
     *
     * search == null / blank:
     * -> lấy toàn bộ Role.
     *
     * GET /api/admin/roles?search=manager
     *
     * -> tìm theo:
     *
     * - roleCode
     * - roleName
     * - description
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(
            String search
    ) {

        List<Roles> roles;


        if (
                search == null
                        || search.trim().isEmpty()
        ) {

            roles =
                    roleRepository
                            .findAll(
                                    Sort.by(
                                            Sort.Direction.ASC,
                                            "id"
                                    )
                            );

        } else {

            roles =
                    roleRepository
                            .searchRoles(
                                    search.trim()
                            );
        }


        return roles
                .stream()

                .map(
                        this::toRoleResponse
                )

                .toList();
    }


    // =========================================================
    // GET ROLE BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(
            Long roleId
    ) {

        Roles role =
                getRoleEntity(
                        roleId
                );


        return toRoleResponse(
                role
        );
    }


    // =========================================================
    // GET USERS OF ROLE
    // =========================================================

    /*
     * GET
     *
     * /api/admin/roles/{roleId}/users
     *
     * Trả danh sách User đang được gán Role này.
     */
    @Transactional(readOnly = true)
    public List<RoleUserResponse> getRoleUsers(
            Long roleId
    ) {

        /*
         * Đảm bảo Role tồn tại.
         *
         * Nếu roleId sai -> 404.
         */
        getRoleEntity(
                roleId
        );


        List<UserRoles> mappings =
                userRoleRepository
                        .findByRole_Id(
                                roleId
                        );


        return mappings
                .stream()

                .map(
                        UserRoles::getUser
                )

                /*
                 * Sắp xếp ổn định theo User ID
                 * để Postman trả kết quả dễ kiểm tra.
                 */
                .sorted(
                        Comparator.comparing(
                                Users::getId
                        )
                )

                .map(
                        this::toRoleUserResponse
                )

                .toList();
    }


    // =========================================================
    // GET ALL PERMISSIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {

        return permissionRepository
                .findAll(
                        Sort.by(
                                Sort.Direction.ASC,
                                "moduleName"
                        )
                        .and(
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "actionName"
                                )
                        )
                )

                .stream()

                .map(
                        this::toPermissionResponse
                )

                .toList();
    }


    // =========================================================
    // GET PERMISSIONS OF ROLE
    // =========================================================

    @Transactional(readOnly = true)
    public RolePermissionResponse getRolePermissions(
            Long roleId
    ) {

        Roles role =
                getRoleEntity(
                        roleId
                );


        List<RolePermissions> mappings =
                rolePermissionRepository
                        .findByRole_IdOrderByPermission_IdAsc(
                                roleId
                        );


        List<PermissionResponse> permissions =
                mappings
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
    // GET ROLES OF USER
    // =========================================================

    /*
     * GET
     *
     * /api/admin/users/{userId}/roles
     *
     * Đây là chiều ngược lại:
     *
     * User -> Roles
     */
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRoles(
            Long userId
    ) {

        /*
         * Đảm bảo User tồn tại.
         */
        if (
                !userRepository.existsById(
                        userId
                )
        ) {

            throw new NotFound(
                    "Không tìm thấy tài khoản"
            );
        }


        List<UserRoles> mappings =
                userRoleRepository
                        .findByUser_Id(
                                userId
                        );


        return mappings
                .stream()

                .map(
                        this::toUserRoleResponse
                )

                .toList();
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
    // ROLE -> ROLE RESPONSE
    // =========================================================

    private RoleResponse toRoleResponse(
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
    // USER -> ROLE USER RESPONSE
    // =========================================================

    /*
     * Dùng cho:
     *
     * Role -> Users
     */
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
    // USER ROLE -> RESPONSE
    // =========================================================

    /*
     * Dùng cho:
     *
     * User -> Roles
     */
    private UserRoleResponse toUserRoleResponse(
            UserRoles userRole
    ) {

        Roles role =
                userRole.getRole();


        return UserRoleResponse
                .builder()

                .userRoleId(
                        userRole.getId()
                )

                .userRolesCode(
                        userRole.getUserRolesCode()
                )

                .roleId(
                        role.getId()
                )

                .roleCode(
                        role.getRoleCode()
                )

                .roleName(
                        role.getRoleName()
                )

                .build();
    }
}