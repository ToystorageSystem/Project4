package com.toystorage.backend.services.stores;

import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.stores.StoreReturnStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.stores.WarehouseReturnItemRepository;
import com.toystorage.backend.repository.stores.WarehouseReturnRepository;

import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReturnedGoodsInspectionValidationService {

    private final UserRepository
            userRepository;

    private final WarehouseReturnRepository
            returnRepository;

    private final WarehouseReturnItemRepository
            returnItemRepository;


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


    public StoreReturns getReturn(
            Long returnId
    ) {

        return returnRepository
                .findById(
                        returnId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Store return not found"
                        )
                );
    }


    public void validateWarehouse(
            Users staff,
            StoreReturns storeReturn
    ) {

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }


        if (!staff
                .getWarehouse()
                .getId()
                .equals(
                        storeReturn
                                .getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "Store return belongs to another warehouse"
            );
        }
    }


    public void validateCanStart(
            StoreReturns storeReturn
    ) {

        if (storeReturn.getStatus()
                != StoreReturnStatus.SHIPPED

                &&

                storeReturn.getStatus()
                        != StoreReturnStatus.INSPECTING) {

            throw new BadRequest(
                    "Store return is not ready for inspection"
            );
        }
    }


    public void validateEditable(
            StoreReturns storeReturn
    ) {

        if (storeReturn.getStatus()
                != StoreReturnStatus.INSPECTING) {

            throw new BadRequest(
                    "Store return is not being inspected"
            );
        }
    }


    public StoreReturnItems getItemByBarcode(
            Long returnId,
            String barcode
    ) {

        return returnItemRepository
                .findByReturnIdAndProductBarcode(
                        returnId,
                        barcode
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Product does not belong to store return"
                        )
                );
    }
}