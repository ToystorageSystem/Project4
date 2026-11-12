
package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.dto.response.warehouses.WarehouseEmployeeResponse;
import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.services.users.ActivityLogService;
import com.toystorage.backend.repository.warehouses.business.WarehouseRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseAssignmentService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ActivityLogService activityLogService;

    // =========================================================
    // GET ASSIGNED EMPLOYEES
    // =========================================================

    @Transactional(readOnly = true)
    public List<WarehouseEmployeeResponse> getEmployees(
            Long warehouseId
    ) {

        Warehouses warehouse = getWarehouse(warehouseId);

        String managerRoleCode = getManagerRoleCode(warehouse);

        return userRepository
                .findByWarehouse_IdOrderByIdAsc(warehouseId)
                .stream()
                .map(user ->
                        toEmployeeResponse(user, managerRoleCode)
                )
                .toList();
    }

    // =========================================================
    // ASSIGN EMPLOYEE TO LOCATION
    // =========================================================

    @Transactional
    public WarehouseEmployeeResponse assignEmployee(
            Long warehouseId,
            Long userId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {

        Warehouses warehouse = getActiveWarehouse(warehouseId);

        Users user = getActiveUser(userId);

        if (
                user.getWarehouse() != null
                        && user.getWarehouse()
                        .getId()
                        .equals(warehouseId)
        ) {
            throw new BadRequest(
                    "Người dùng đã được phân công tại địa điểm này"
            );
        }

        if (user.getWarehouse() != null) {
            throw new BadRequest(
                    "Người dùng đang được phân công tại địa điểm khác"
            );
        }

        Map<String, Object> oldValue =
                assignmentSnapshot(user);

        user.setWarehouse(warehouse);

        Users saved = userRepository.saveAndFlush(user);

        activityLogService.log(
                authentication,
                httpRequest,
                ActivityAction.UPDATE,
                ActivityEntityType.WAREHOUSE,
                warehouse.getId(),
                oldValue,
                assignmentSnapshot(saved)
        );

        return toEmployeeResponse(
                saved,
                getManagerRoleCode(warehouse)
        );
    }

    // =========================================================
    // UNASSIGN EMPLOYEE
    // =========================================================

    @Transactional
    public WarehouseEmployeeResponse unassignEmployee(
            Long warehouseId,
            Long userId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {

        Warehouses warehouse = getWarehouse(warehouseId);

        Users user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy người dùng"
                        )
                );

        if (
                user.getWarehouse() == null
                        || !user.getWarehouse()
                        .getId()
                        .equals(warehouseId)
        ) {
            throw new BadRequest(
                    "Người dùng không được phân công tại địa điểm này"
            );
        }

        String managerRoleCode = getManagerRoleCode(warehouse);

        /*
         * Không cho unassign trực tiếp manager.
         *
         * Nếu muốn đổi manager:
         * Admin phải gán manager khác trước,
         * hoặc làm endpoint remove manager riêng.
         */
        if (hasRole(userId, managerRoleCode)) {
            throw new BadRequest(
                    "Không thể bỏ phân công người quản lý bằng chức năng nhân viên"
            );
        }

        Map<String, Object> oldValue =
                assignmentSnapshot(user);

        user.setWarehouse(null);

        Users saved = userRepository.saveAndFlush(user);

        activityLogService.log(
                authentication,
                httpRequest,
                ActivityAction.UPDATE,
                ActivityEntityType.WAREHOUSE,
                warehouseId,
                oldValue,
                assignmentSnapshot(saved)
        );

        return toEmployeeResponse(
                saved,
                managerRoleCode
        );
    }

    // =========================================================
    // ASSIGN MANAGER
    // =========================================================

    @Transactional
    public WarehouseEmployeeResponse assignManager(
            Long warehouseId,
            Long userId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {

        Warehouses warehouse = getActiveWarehouse(warehouseId);

        Users newManager = getActiveUser(userId);

        String requiredRoleCode =
                getManagerRoleCode(warehouse);

        // =====================================================
        // USER MUST HAVE CORRECT MANAGER ROLE
        // =====================================================

        if (!hasRole(userId, requiredRoleCode)) {
            throw new BadRequest(
                    warehouse.getType()
                            == WarehouseType.MAIN_WAREHOUSE
                            ? "Người quản lý kho tổng phải có vai trò WAREHOUSE_MANAGER"
                            : "Người quản lý cửa hàng phải có vai trò STORE_MANAGER"
            );
        }

        // =====================================================
        // MANAGER CANNOT CURRENTLY BELONG TO ANOTHER LOCATION
        // =====================================================

        if (
                newManager.getWarehouse() != null
                        && !newManager.getWarehouse()
                        .getId()
                        .equals(warehouseId)
        ) {
            throw new BadRequest(
                    "Người dùng đang được phân công tại địa điểm khác"
            );
        }

        // =====================================================
        // FIND CURRENT MANAGER
        // =====================================================

        List<Users> currentManagers =
                userRepository
                        .findByWarehouse_IdOrderByIdAsc(warehouseId)
                        .stream()
                        .filter(user ->
                                hasRole(user.getId(), requiredRoleCode)
                        )
                        .toList();

        /*
         * Kiểm tra nếu người dùng đã là manager.
         */
        boolean alreadyManager =
                currentManagers
                        .stream()
                        .anyMatch(manager ->
                                manager.getId().equals(userId)
                        );

        if (alreadyManager) {
            throw new BadRequest(
                    "Người dùng đã là người quản lý của địa điểm này"
            );
        }

        /*
         * Một địa điểm chỉ có một manager.
         *
         * Manager cũ vẫn giữ Role,
         * chỉ bỏ warehouse assignment.
         */
        for (Users currentManager : currentManagers) {

            currentManager.setWarehouse(null);

            userRepository.save(currentManager);
        }

        Map<String, Object> oldValue =
                new LinkedHashMap<>();

        oldValue.put(
                "warehouseId",
                warehouseId
        );

        oldValue.put(
                "managerUserIds",
                currentManagers
                        .stream()
                        .map(Users::getId)
                        .toList()
        );

        newManager.setWarehouse(warehouse);

        Users saved = userRepository
                .saveAndFlush(newManager);

        Map<String, Object> newValue =
                new LinkedHashMap<>();

        newValue.put(
                "warehouseId",
                warehouseId
        );

        newValue.put(
                "managerUserId",
                saved.getId()
        );

        newValue.put(
                "managerUserCode",
                saved.getUserCode()
        );

        activityLogService.log(
                authentication,
                httpRequest,
                ActivityAction.UPDATE,
                ActivityEntityType.WAREHOUSE,
                warehouseId,
                oldValue,
                newValue
        );

        return toEmployeeResponse(
                saved,
                requiredRoleCode
        );
    }

    // =========================================================
    // GET WAREHOUSE
    // =========================================================

    private Warehouses getWarehouse(
            Long warehouseId
    ) {

        return warehouseRepository
                .findById(warehouseId)
                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy kho hoặc cửa hàng"
                        )
                );
    }

    // =========================================================
    // GET ACTIVE WAREHOUSE
    // =========================================================

    private Warehouses getActiveWarehouse(
            Long warehouseId
    ) {

        Warehouses warehouse = getWarehouse(warehouseId);

        if (
                warehouse.getStatus()
                        != WarehouseStatus.ACTIVE
        ) {
            throw new BadRequest(
                    "Không thể phân công nhân sự vào địa điểm INACTIVE"
            );
        }

        return warehouse;
    }

    // =========================================================
    // GET ACTIVE USER
    // =========================================================

    private Users getActiveUser(
            Long userId
    ) {

        Users user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy người dùng"
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequest(
                    "Chỉ có thể phân công người dùng ACTIVE"
            );
        }

        return user;
    }

    // =========================================================
    // MANAGER ROLE BY LOCATION TYPE
    // =========================================================

    // Lay dung ma role dang luu trong MySQL
    private String getManagerRoleCode(
            Warehouses warehouse
    ) {

        if (
                warehouse.getType()
                        == WarehouseType.MAIN_WAREHOUSE
        ) {
            return "ROLE-WH-MANAGER";
        }

        return "ROLE-STORE-MANAGER";
    }

    // =========================================================
    // ROLE MATCHING
    // =========================================================

    /*
     * So khớp role code không phân biệt kiểu đặt tên:
     * "ROLE-STORE-MANAGER", "ROLE_STORE_MANAGER", "STORE_MANAGER"
     * đều được coi là "STORE_MANAGER" (giống normalizeRole ở frontend).
     *
     * DB dang luu "WAREHOUSE_MANAGER"/"WAREHOUSE_STAFF", con code dung
     * "ROLE-WH-MANAGER"/"ROLE-WH-STAFF" => coi WAREHOUSE_* va WH_* la mot.
     */
    private static String normalizeRole(String code) {
        if (code == null) {
            return "";
        }
        return code
                .trim()
                .toUpperCase()
                .replaceFirst("^ROLE[-_]", "")
                .replace('-', '_')
                .replaceFirst("^WAREHOUSE_", "WH_");
    }

    private boolean hasRole(Long userId, String roleCode) {
        String wanted = normalizeRole(roleCode);

        return userRoleRepository
                .findByUser_Id(userId)
                .stream()
                .filter(userRole -> userRole.getRole() != null)
                .anyMatch(userRole ->
                        normalizeRole(
                                userRole.getRole().getRoleCode()
                        ).equals(wanted)
                );
    }

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    private WarehouseEmployeeResponse toEmployeeResponse(
            Users user,
            String managerRoleCode
    ) {

        List<UserRoles> userRoles =
                userRoleRepository
                        .findByUser_Id(user.getId());

        List<String> roleCodes =
                userRoles
                        .stream()
                        .filter(userRole ->
                                userRole.getRole() != null
                        )
                        .map(userRole ->
                                userRole
                                        .getRole()
                                        .getRoleCode()
                        )
                        .toList();

        String wanted = normalizeRole(managerRoleCode);

        boolean manager =
                roleCodes
                        .stream()
                        .anyMatch(code ->
                                normalizeRole(code).equals(wanted)
                        );

        return WarehouseEmployeeResponse
                .builder()
                .userId(user.getId())
                .userCode(user.getUserCode())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .warehouseId(
                        user.getWarehouse() != null
                                ? user.getWarehouse().getId()
                                : null
                )
                .roles(roleCodes)
                .manager(manager)
                .build();
    }

    // =========================================================
    // LOG SNAPSHOT
    // =========================================================

    private Map<String, Object> assignmentSnapshot(
            Users user
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "userId",
                user.getId()
        );

        data.put(
                "userCode",
                user.getUserCode()
        );

        data.put(
                "warehouseId",
                user.getWarehouse() != null
                        ? user.getWarehouse().getId()
                        : null
        );

        return data;
    }
}
