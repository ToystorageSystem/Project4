package com.toystorage.backend.stockcount;

import com.toystorage.backend.entity.auth.Roles;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Authoritative, warehouse-scoped access check for Manager stock-count endpoints.
 *
 * The database role assignment is the source of truth. Session authorities can
 * be stale after importing demo data or changing a user's role, which previously
 * caused inconsistent checks between endpoints and workflow actions.
 *
 * Only an ACTIVE user assigned to a MAIN_WAREHOUSE with the WAREHOUSE_MANAGER
 * role is allowed. This NEVER grants Staff or Store Manager access.
 */
@Component("stockCountManagerAccess")
@RequiredArgsConstructor
public class StockCountManagerAccess {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public boolean canManage(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return false;
        }

        Users user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || user.getWarehouse() == null
                || user.getWarehouse().getType() != WarehouseType.MAIN_WAREHOUSE) {
            return false;
        }

        return userRoleRepository.findByUser_Id(user.getId()).stream()
                .map(userRole -> userRole.getRole())
                .anyMatch(this::isWarehouseManager);
    }

    private boolean isWarehouseManager(Roles role) {
        if (role == null) {
            return false;
        }
        // Accept the canonical code or the equivalent friendly role name.
        // Do not infer permissions from broad labels such as "Warehouse".
        return "WAREHOUSE_MANAGER".equals(normalize(role.getRoleCode()))
                || "WAREHOUSE_MANAGER".equals(normalize(role.getRoleName()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "_");
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
