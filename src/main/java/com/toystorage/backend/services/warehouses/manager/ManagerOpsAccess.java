package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kiem tra quyen cho cac API quan ly kho moi (phan cong, hieu suat, bao cao sap het hang).
 * Ten role duoc chuan hoa: bo tien to ROLE_ (lap lai), doi '-' / khoang trang thanh '_'.
 * Dung trong @PreAuthorize("@managerOpsAccess.isManager(authentication)").
 */
@Component("managerOpsAccess")
@RequiredArgsConstructor
public class ManagerOpsAccess {

    public static final Set<String> MANAGER_ROLES = Set.of("WAREHOUSE_MANAGER", "WH_MANAGER");
    public static final Set<String> STAFF_ROLES = Set.of("WAREHOUSE_STAFF", "WH_STAFF");
    public static final Set<String> BUSINESS_ROLES = Set.of(
            "BIZ_OPS", "BUSINESS_OPERATIONS", "BIZ_MANAGER", "BUSINESS_MANAGER",
            "BIZ_STAFF", "BUSINESS_STAFF", "ADMIN", "ADMINISTRATOR");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public boolean isManager(Authentication auth) { return hasAny(auth, MANAGER_ROLES); }

    public boolean isStaff(Authentication auth) { return hasAny(auth, STAFF_ROLES); }

    public boolean isBusiness(Authentication auth) { return hasAny(auth, BUSINESS_ROLES); }

    private boolean hasAny(Authentication auth, Set<String> roles) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (roles.contains(normalize(a.getAuthority()))) return true;
        }
        return false;
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String v = value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        while (v.startsWith("ROLE_")) v = v.substring(5);
        return v;
    }

    /** User dang dang nhap. */
    @Transactional(readOnly = true)
    public Users currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        Users user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return user;
    }

    /** User dang dang nhap va bat buoc da duoc gan kho. */
    @Transactional(readOnly = true)
    public Users currentWarehouseUser() {
        Users user = currentUser();
        if (user.getWarehouse() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No warehouse is assigned to your account");
        }
        return user;
    }

    /** Nhan vien kho (role Warehouse Staff) thuoc cung kho voi manager. */
    @Transactional(readOnly = true)
    public List<Users> staffOfWarehouse(Long warehouseId) {
        return userRepository.findByWarehouse_IdOrderByIdAsc(warehouseId).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> userRoleRepository.findByUser_Id(u.getId()).stream()
                        .map(ur -> ur.getRole())
                        .anyMatch(this::isStaffRole))
                .toList();
    }

    private boolean isStaffRole(Roles role) {
        return role != null && (STAFF_ROLES.contains(normalize(role.getRoleCode()))
                || STAFF_ROLES.contains(normalize(role.getRoleName())));
    }
}
