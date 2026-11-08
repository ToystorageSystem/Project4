package com.toystorage.backend.services.stores;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.stores.StoreRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Access check for every /api/store/** feature (search, shelves, sales).
 * Done in the service (not @PreAuthorize) because Global turns
 * AccessDeniedException into 500; Forbidden is mapped to 403 with a message.
 */
@Service
@RequiredArgsConstructor
public class StoreAccessService {

    /** roles.roles_code allowed to use the store app */
    private static final Set<String> STORE_ROLE_CODES =
            Set.of("ROLE-STORE-STAFF", "ROLE-STORE-MANAGER");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final StoreRepository storeRepository;
  private static final String STORE_MANAGER_ROLE = "ROLE-STORE-MANAGER";



       /**
     * @return the current user (managed entity) after checking store role + store.
     * Used by write operations that must record "who did it".
     */
    @Transactional(readOnly = true)
    public Users requireStoreUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new Unauthorized("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new Forbidden("Access denied");
        }

        boolean hasStoreRole = userRoleRepository.findByUser_Id(user.getId()).stream()
                .anyMatch(ur -> ur.getRole() != null
                        && STORE_ROLE_CODES.contains(ur.getRole().getRoleCode()));
        if (!hasStoreRole) {
            throw new Forbidden("Access denied");
        }

        Warehouses warehouse = user.getWarehouse();
        if (warehouse == null
                || warehouse.getType() != WarehouseType.STORE
                || !storeRepository.existsById(warehouse.getId())) {
            throw new Forbidden("User is not assigned to a store");
        }
        return user;
    }

    /** @return id of the store (= warehouses.id = stores.warehouse_id) the user works in */
    @Transactional(readOnly = true)
    public Long requireStoreId(Long userId) {
        return requireStoreUser(userId).getWarehouse().getId();
    }


    /** store user AND Store Manager role (confirm receiving, …) */
    @Transactional(readOnly = true)
    public Users requireStoreManager(Long userId) {
        Users user = requireStoreUser(userId);
        boolean isManager = userRoleRepository.findByUser_Id(user.getId()).stream()
                .anyMatch(ur -> ur.getRole() != null
                        && STORE_MANAGER_ROLE.equals(ur.getRole().getRoleCode()));
        if (!isManager) {
            throw new Forbidden("Only store manager can confirm");
        }
        return user;
    }
}