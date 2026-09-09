package com.toystorage.backend.services.users;

import com.toystorage.backend.dto.request.users.CreateUserRequest;
import com.toystorage.backend.dto.request.users.ResetPasswordRequest;
import com.toystorage.backend.dto.request.users.UpdateUserRequest;
import com.toystorage.backend.dto.request.users.UpdateUserRolesRequest;

import com.toystorage.backend.dto.response.auth.UserRoleResponse;
import com.toystorage.backend.dto.response.users.UserResponse;

import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.auth.RoleRepository;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.WarehouseRepository;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final RoleRepository roleRepository;

    private final WarehouseRepository warehouseRepository;

    private final PasswordEncoder passwordEncoder;

    private final ActivityLogService activityLogService;


    // =========================================================
    // CREATE USER
    // =========================================================

    @Transactional
    public UserResponse createUser(
            CreateUserRequest request
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();


        String identityCard =
                request.getIdentityCard()
                        .trim();


        // -----------------------------------------------------
        // EMAIL UNIQUE
        // -----------------------------------------------------

        if (
                userRepository
                        .existsByEmailIgnoreCase(
                                email
                        )
        ) {

            throw new BadRequest(
                    "Email đã tồn tại trong hệ thống"
            );
        }


        // -----------------------------------------------------
        // CCCD UNIQUE
        // -----------------------------------------------------

        if (
                userRepository
                        .existsByIdentityCard(
                                identityCard
                        )
        ) {

            throw new BadRequest(
                    "CCCD đã tồn tại trong hệ thống"
            );
        }


        // -----------------------------------------------------
        // ROLE
        // -----------------------------------------------------

        if (
                request.getRoleIds() == null
                        || request.getRoleIds().isEmpty()
        ) {

            throw new BadRequest(
                    "Người dùng phải có ít nhất một vai trò"
            );
        }


        List<Long> roleIds =
                request.getRoleIds()
                        .stream()
                        .distinct()
                        .toList();


        List<Roles> roles =
                roleRepository
                        .findAllById(
                                roleIds
                        );


        if (
                roles.size()
                        != roleIds.size()
        ) {

            throw new BadRequest(
                    "Có vai trò không tồn tại"
            );
        }


        // -----------------------------------------------------
        // WAREHOUSE / STORE
        // -----------------------------------------------------

        Warehouses warehouse =
                getActiveWarehouse(
                        request.getWarehouseId()
                );


        // -----------------------------------------------------
        // AUTO USER CODE
        // -----------------------------------------------------

        String userCode =
                generateUserCode();


        // -----------------------------------------------------
        // CREATE USER ENTITY
        // -----------------------------------------------------

        Users user =
                new Users();


        user.setUserCode(
                userCode
        );


        user.setName(
                request.getName()
                        .trim()
        );


        user.setEmail(
                email
        );


        user.setPhone(
                normalizeNullable(
                        request.getPhone()
                )
        );


        user.setIdentityCard(
                identityCard
        );


        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );


        user.setStatus(
                UserStatus.ACTIVE
        );


        /*
         * User mới bắt buộc đổi password
         * ở lần đăng nhập đầu tiên.
         */
        user.setMustChangePassword(
                true
        );


        user.setWarehouse(
                warehouse
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        // -----------------------------------------------------
        // ASSIGN EXISTING ROLES
        // -----------------------------------------------------

        saveUserRoles(
                savedUser,
                roles
        );


        // -----------------------------------------------------
        // ACTIVITY LOG - CREATE USER
        // -----------------------------------------------------

        Map<String, Object> newValue =
                buildSafeUserSnapshot(
                        savedUser
                );


        newValue.put(
                "roleIds",
                roleIds
        );


        logUserActivity(
                ActivityAction.CREATE,
                savedUser.getId(),
                null,
                newValue
        );


        return toResponse(
                savedUser
        );
    }


    // =========================================================
    // GET USER BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public UserResponse getUserById(
            Long userId
    ) {

        Users user =
                getUserEntity(
                        userId
                );


        return toResponse(
                user
        );
    }


    // =========================================================
    // LIST + SEARCH + FILTER
    // =========================================================

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(

            String keyword,

            Long roleId,

            Long warehouseId,

            UserStatus status,

            Pageable pageable
    ) {

        String normalizedKeyword =
                keyword == null
                        ? null
                        : keyword.trim();


        return userRepository
                .searchUsers(
                        normalizedKeyword,
                        roleId,
                        warehouseId,
                        status,
                        pageable
                )

                .map(
                        this::toResponse
                );
    }


    // =========================================================
    // UPDATE USER
    // =========================================================

    /*
     * Update thông tin cơ bản.
     *
     * KHÔNG thay đổi Role.
     */
    @Transactional
    public UserResponse updateUser(

            Long userId,

            UpdateUserRequest request
    ) {

        Users user =
                getUserEntity(
                        userId
                );


        // -----------------------------------------------------
        // OLD VALUE
        // -----------------------------------------------------

        Map<String, Object> oldValue =
                buildSafeUserSnapshot(
                        user
                );


        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();


        String identityCard =
                request.getIdentityCard()
                        .trim();


        // -----------------------------------------------------
        // EMAIL UNIQUE
        // -----------------------------------------------------

        if (
                userRepository
                        .existsByEmailIgnoreCaseAndIdNot(
                                email,
                                userId
                        )
        ) {

            throw new BadRequest(
                    "Email đã tồn tại trong hệ thống"
            );
        }


        // -----------------------------------------------------
        // CCCD UNIQUE
        // -----------------------------------------------------

        if (
                userRepository
                        .existsByIdentityCardAndIdNot(
                                identityCard,
                                userId
                        )
        ) {

            throw new BadRequest(
                    "CCCD đã tồn tại trong hệ thống"
            );
        }


        // -----------------------------------------------------
        // WAREHOUSE
        // -----------------------------------------------------

        Warehouses warehouse =
                getActiveWarehouse(
                        request.getWarehouseId()
                );


        // -----------------------------------------------------
        // UPDATE
        // -----------------------------------------------------

        user.setName(
                request.getName()
                        .trim()
        );


        user.setEmail(
                email
        );


        user.setPhone(
                normalizeNullable(
                        request.getPhone()
                )
        );


        user.setIdentityCard(
                identityCard
        );


        user.setWarehouse(
                warehouse
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        // -----------------------------------------------------
        // ACTIVITY LOG
        // -----------------------------------------------------

        Map<String, Object> newValue =
                buildSafeUserSnapshot(
                        savedUser
                );


        logUserActivity(
                ActivityAction.UPDATE,
                savedUser.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedUser
        );
    }


    // =========================================================
    // UPDATE USER ROLES
    // =========================================================

    /*
     * Cập nhật toàn bộ danh sách Role của User.
     *
     * Ví dụ:
     *
     * Hiện tại:
     * BUSINESS_STAFF
     *
     * Request:
     * [BUSINESS_STAFF, AUDITOR]
     *
     * => chỉ ADD AUDITOR.
     *
     *
     * Request tiếp:
     * [BUSINESS_STAFF]
     *
     * => chỉ REMOVE AUDITOR.
     *
     *
     * Không xóa toàn bộ mapping rồi tạo lại.
     *
     * Chỉ thêm Role mới và xóa Role thực sự bị bỏ.
     */
    @Transactional
    public List<UserRoleResponse> updateUserRoles(

            Long userId,

            UpdateUserRolesRequest request
    ) {

        // -----------------------------------------------------
        // 1. USER PHẢI TỒN TẠI
        // -----------------------------------------------------

        Users user =
                getUserEntity(
                        userId
                );


        // -----------------------------------------------------
        // 2. PHẢI CÓ ÍT NHẤT 1 ROLE
        // -----------------------------------------------------

        if (
                request.getRoleIds() == null
                        || request.getRoleIds().isEmpty()
        ) {

            throw new BadRequest(
                    "Người dùng phải có ít nhất một vai trò"
            );
        }


        /*
         * Loại bỏ ID trùng.
         *
         * [8, 8, 6]
         *
         * =>
         *
         * [8, 6]
         */
        Set<Long> requestedRoleIds =
                new HashSet<>(
                        request.getRoleIds()
                );


        // -----------------------------------------------------
        // 3. TẤT CẢ ROLE PHẢI TỒN TẠI
        // -----------------------------------------------------

        List<Roles> requestedRoles =
                roleRepository
                        .findAllById(
                                requestedRoleIds
                        );


        if (
                requestedRoles.size()
                        != requestedRoleIds.size()
        ) {

            throw new BadRequest(
                    "Có vai trò không tồn tại"
            );
        }


        // -----------------------------------------------------
        // 4. ROLE HIỆN TẠI
        // -----------------------------------------------------

        List<UserRoles> currentMappings =
                userRoleRepository
                        .findByUser_Id(
                                userId
                        );


        Set<Long> currentRoleIds =
                new HashSet<>();


        for (UserRoles mapping : currentMappings) {

            currentRoleIds.add(
                    mapping
                            .getRole()
                            .getId()
            );
        }


        // -----------------------------------------------------
        // 5. BẢO VỆ ADMIN CUỐI CÙNG
        // -----------------------------------------------------

        boolean currentlyAdmin =
                currentMappings
                        .stream()

                        .anyMatch(mapping ->
                                "ADMIN".equals(
                                        mapping
                                                .getRole()
                                                .getRoleCode()
                                )
                        );


        boolean willStillBeAdmin =
                requestedRoles
                        .stream()

                        .anyMatch(role ->
                                "ADMIN".equals(
                                        role.getRoleCode()
                                )
                        );


        /*
         * Nếu User hiện đang là ADMIN,
         * nhưng request mới không còn ADMIN,
         * thì phải kiểm tra Admin cuối cùng.
         */
        if (
                currentlyAdmin
                        && !willStillBeAdmin
                        && user.getStatus() == UserStatus.ACTIVE
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
                        "Không thể gỡ vai trò ADMIN khỏi Admin cuối cùng"
                );
            }
        }


        // -----------------------------------------------------
        // 6. ADD ROLE MỚI
        // -----------------------------------------------------

        for (Roles role : requestedRoles) {

            /*
             * Role này đã tồn tại trên User
             * thì giữ nguyên.
             */
            if (
                    currentRoleIds.contains(
                            role.getId()
                    )
            ) {

                continue;
            }


            UserRoles newMapping =
                    new UserRoles();


            newMapping.setUser(
                    user
            );


            newMapping.setRole(
                    role
            );


            /*
             * user_roles_code trong DB
             * là NOT NULL.
             */
            newMapping.setUserRolesCode(
                    generateUserRoleCode()
            );


            userRoleRepository.save(
                    newMapping
            );


            // ---------------------------------------------
            // ACTIVITY LOG - ASSIGN ROLE
            // ---------------------------------------------

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
                    "roleName",
                    role.getRoleName()
            );


            logUserActivity(
                    ActivityAction.ASSIGN_ROLE,
                    userId,
                    null,
                    newValue
            );
        }


        // -----------------------------------------------------
        // 7. REMOVE ROLE KHÔNG CÒN TRONG REQUEST
        // -----------------------------------------------------

        for (UserRoles mapping : currentMappings) {

            Roles role =
                    mapping.getRole();


            /*
             * Nếu request vẫn chứa Role này
             * thì giữ nguyên.
             */
            if (
                    requestedRoleIds.contains(
                            role.getId()
                    )
            ) {

                continue;
            }


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
                    "roleName",
                    role.getRoleName()
            );


            userRoleRepository.delete(
                    mapping
            );


            // ---------------------------------------------
            // ACTIVITY LOG - REMOVE ROLE
            // ---------------------------------------------

            logUserActivity(
                    ActivityAction.REMOVE_ROLE,
                    userId,
                    oldValue,
                    null
            );
        }


        /*
         * Đẩy INSERT/DELETE xuống persistence context
         * trước khi query lại danh sách mới.
         */
        userRoleRepository.flush();


        // -----------------------------------------------------
        // 8. RETURN UPDATED ROLES
        // -----------------------------------------------------

        List<UserRoles> updatedMappings =
                userRoleRepository
                        .findByUser_Id(
                                userId
                        );


        return updatedMappings
                .stream()

                .map(
                        this::toUserRoleResponse
                )

                .toList();
    }


    // =========================================================
    // LOCK USER
    // =========================================================

    @Transactional
    public UserResponse lockUser(
            Long userId
    ) {

        Users user =
                getUserEntity(
                        userId
                );


        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Chỉ tài khoản ACTIVE mới có thể bị khóa"
            );
        }


        /*
         * Không khóa Admin cuối cùng.
         */
        protectLastActiveAdmin(
                userId
        );


        UserStatus oldStatus =
                user.getStatus();


        user.setStatus(
                UserStatus.LOCKED
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "status",
                oldStatus.name()
        );


        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "status",
                savedUser
                        .getStatus()
                        .name()
        );


        logUserActivity(
                ActivityAction.UPDATE,
                savedUser.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedUser
        );
    }


    // =========================================================
    // UNLOCK USER
    // =========================================================

    @Transactional
    public UserResponse unlockUser(
            Long userId
    ) {

        Users user =
                getUserEntity(
                        userId
                );


        if (
                user.getStatus()
                        != UserStatus.LOCKED
        ) {

            throw new BadRequest(
                    "Tài khoản hiện không ở trạng thái LOCKED"
            );
        }


        UserStatus oldStatus =
                user.getStatus();


        user.setStatus(
                UserStatus.ACTIVE
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "status",
                oldStatus.name()
        );


        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "status",
                savedUser
                        .getStatus()
                        .name()
        );


        logUserActivity(
                ActivityAction.UPDATE,
                savedUser.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedUser
        );
    }


    // =========================================================
    // DEACTIVATE USER
    // =========================================================

    @Transactional
    public UserResponse deactivateUser(
            Long userId
    ) {

        Users user =
                getUserEntity(
                        userId
                );


        if (
                user.getStatus()
                        == UserStatus.INACTIVE
        ) {

            throw new BadRequest(
                    "Tài khoản đã ngừng sử dụng"
            );
        }


        /*
         * Không vô hiệu hóa
         * Admin ACTIVE cuối cùng.
         */
        protectLastActiveAdmin(
                userId
        );


        UserStatus oldStatus =
                user.getStatus();


        user.setStatus(
                UserStatus.INACTIVE
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "status",
                oldStatus.name()
        );


        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "status",
                UserStatus.INACTIVE.name()
        );


        logUserActivity(
                ActivityAction.UPDATE,
                savedUser.getId(),
                oldValue,
                newValue
        );


        return toResponse(
                savedUser
        );
    }


    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @Transactional
    public void resetPassword(

            Long userId,

            ResetPasswordRequest request
    ) {

        Users user =
                getUserEntity(
                        userId
                );


        /*
         * Tuyệt đối không ghi:
         *
         * - password cũ
         * - password mới
         * - BCrypt hash
         */
        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "mustChangePassword",
                user.isMustChangePassword()
        );


        String encodedPassword =
                passwordEncoder.encode(
                        request.getNewPassword()
                );


        user.setPassword(
                encodedPassword
        );


        user.setMustChangePassword(
                true
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "passwordReset",
                true
        );


        newValue.put(
                "mustChangePassword",
                savedUser.isMustChangePassword()
        );


        logUserActivity(
                ActivityAction.UPDATE,
                savedUser.getId(),
                oldValue,
                newValue
        );
    }


    // =========================================================
    // GET USER ENTITY
    // =========================================================

    private Users getUserEntity(
            Long userId
    ) {

        return userRepository
                .findById(
                        userId
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy tài khoản"
                        )
                );
    }


    // =========================================================
    // GET ACTIVE WAREHOUSE
    // =========================================================

    private Warehouses getActiveWarehouse(
            Long warehouseId
    ) {

        if (warehouseId == null) {

            return null;
        }


        Warehouses warehouse =
                warehouseRepository
                        .findById(
                                warehouseId
                        )

                        .orElseThrow(() ->
                                new NotFound(
                                        "Không tìm thấy kho hoặc cửa hàng"
                                )
                        );


        if (
                warehouse.getStatus()
                        != WarehouseStatus.ACTIVE
        ) {

            throw new BadRequest(
                    "Kho hoặc cửa hàng đang không hoạt động"
            );
        }


        return warehouse;
    }


    // =========================================================
    // SAVE USER ROLES
    // =========================================================

    /*
     * Chỉ dùng trong CREATE USER.
     */
    private void saveUserRoles(

            Users user,

            List<Roles> roles
    ) {

        for (Roles role : roles) {

            UserRoles userRole =
                    new UserRoles();


            userRole.setUser(
                    user
            );


            userRole.setRole(
                    role
            );


            userRole.setUserRolesCode(
                    generateUserRoleCode()
            );


            userRoleRepository.save(
                    userRole
            );
        }
    }


    // =========================================================
    // GENERATE USER ROLE CODE
    // =========================================================

    private String generateUserRoleCode() {

        return "UR-"
                + UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }


    // =========================================================
    // PROTECT LAST ACTIVE ADMIN
    // =========================================================

    private void protectLastActiveAdmin(
            Long userId
    ) {

        boolean isAdmin =
                userRoleRepository
                        .existsByUser_IdAndRole_RoleCode(
                                userId,
                                "ADMIN"
                        );


        if (!isAdmin) {

            return;
        }


        Users user =
                getUserEntity(
                        userId
                );


        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            return;
        }


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
                    "Không thể khóa hoặc vô hiệu hóa Admin cuối cùng"
            );
        }
    }


    // =========================================================
    // GENERATE USER CODE
    // =========================================================

    private String generateUserCode() {

        Long max =
                userRepository
                        .findMaxUserCodeNumber();


        if (max == null) {

            max = 0L;
        }


        long next =
                max + 1;


        return String.format(
                "USR%03d",
                next
        );
    }


    // =========================================================
    // ACTIVITY LOG
    // =========================================================

    private void logUserActivity(

            ActivityAction action,

            Long targetUserId,

            Object oldValue,

            Object newValue
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        HttpServletRequest httpRequest =
                getCurrentHttpRequest();


        activityLogService.log(

                authentication,

                httpRequest,

                action,

                ActivityEntityType.USER,

                targetUserId,

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


    // =========================================================
    // SAFE USER SNAPSHOT
    // =========================================================

    /*
     * Không chứa:
     *
     * password
     * password hash
     * CCCD
     */
    private Map<String, Object> buildSafeUserSnapshot(
            Users user
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();


        data.put(
                "userCode",
                user.getUserCode()
        );


        data.put(
                "name",
                user.getName()
        );


        data.put(
                "email",
                user.getEmail()
        );


        data.put(
                "phone",
                user.getPhone()
        );


        data.put(
                "status",
                user.getStatus() != null
                        ? user.getStatus().name()
                        : null
        );


        data.put(
                "mustChangePassword",
                user.isMustChangePassword()
        );


        Warehouses warehouse =
                user.getWarehouse();


        data.put(
                "warehouseId",
                warehouse != null
                        ? warehouse.getId()
                        : null
        );


        data.put(
                "warehouseCode",
                warehouse != null
                        ? warehouse.getWarehousesCode()
                        : null
        );


        return data;
    }


    // =========================================================
    // NORMALIZE NULLABLE STRING
    // =========================================================

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {

            return null;
        }


        String normalized =
                value.trim();


        return normalized.isEmpty()
                ? null
                : normalized;
    }


    // =========================================================
    // USER ROLE -> RESPONSE
    // =========================================================

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


    // =========================================================
    // USER ENTITY -> RESPONSE
    // =========================================================

    private UserResponse toResponse(
            Users user
    ) {

        List<UserRoles> userRoles =
                userRoleRepository
                        .findByUser_Id(
                                user.getId()
                        );


        List<UserResponse.RoleInfo> roleResponses =
                new ArrayList<>();


        for (UserRoles userRole : userRoles) {

            Roles role =
                    userRole.getRole();


            UserResponse.RoleInfo roleInfo =
                    UserResponse.RoleInfo
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

                            .build();


            roleResponses.add(
                    roleInfo
            );
        }


        Warehouses warehouse =
                user.getWarehouse();


        return UserResponse
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

                .identityCard(
                        user.getIdentityCard()
                )

                .status(
                        user.getStatus()
                )

                .mustChangePassword(
                        user.isMustChangePassword()
                )

                .warehouseId(
                        warehouse != null
                                ? warehouse.getId()
                                : null
                )

                .warehouseCode(
                        warehouse != null
                                ? warehouse.getWarehousesCode()
                                : null
                )

                .warehouseName(
                        warehouse != null
                                ? warehouse.getName()
                                : null
                )

                .roles(
                        roleResponses
                )

                .createdAt(
                        user.getCreatedAt()
                )

                .updatedAt(
                        user.getUpdatedAt()
                )

                .build();
    }
}