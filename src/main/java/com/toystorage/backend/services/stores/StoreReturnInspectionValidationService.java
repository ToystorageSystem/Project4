package com.toystorage.backend.services.stores;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.stores.StoreReturnRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreReturnInspectionValidationService {

    private final UserRepository userRepository;

    private final StoreReturnRepository
            storeReturnRepository;


    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    public Long getWarehouseId(
            Users manager
    ) {

        if (manager.getWarehouse() == null) {

            throw new Forbidden(
                    "Manager is not assigned to a warehouse"
            );
        }

        return manager
                .getWarehouse()
                .getId();
    }


    public StoreReturns getReturn(
            Long returnId
    ) {

        return storeReturnRepository
                .findById(returnId)

                .orElseThrow(() ->
                        new NotFound(
                                "Store return not found: "
                                        + returnId
                        )
                );
    }


    public void validateWarehouse(
            Users manager,
            StoreReturns storeReturn
    ) {

        Long warehouseId =
                getWarehouseId(manager);

        if (storeReturn.getWarehouse() == null
                || !warehouseId.equals(
                storeReturn
                        .getWarehouse()
                        .getId()
        )) {

            throw new Forbidden(
                    "You cannot inspect store return "
                            + "from another warehouse"
            );
        }
    }


    public void validateCanReceive(
            StoreReturns storeReturn
    ) {

        String status =
                storeReturn.getStatus().name();

        if (!"SHIPPED".equals(status)
                && !"RECEIVED".equals(status)) {

            throw new BadRequest(
                    "Store return must be SHIPPED "
                            + "before warehouse inspection"
            );
        }
    }
}