package com.toystorage.backend.services.inventories.stockcount.staff;

import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.inventories.stockcount.StaffStockCountItemRepository;
import com.toystorage.backend.repository.inventories.stockcount.StaffStockCountRepository;

import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffStockCountValidationService {

    private final UserRepository
            userRepository;

    private final StaffStockCountRepository
            stockCountRepository;

    private final StaffStockCountItemRepository
            stockCountItemRepository;


    // =====================================================
    // CURRENT USER
    // =====================================================

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


    // =====================================================
    // STOCK COUNT
    // =====================================================

    public StockCounts getStockCount(
            Long stockCountId
    ) {

        return stockCountRepository
                .findById(
                        stockCountId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Stock count not found"
                        )
                );
    }


    // =====================================================
    // WAREHOUSE
    // =====================================================

    public void validateWarehouse(
            Users staff,
            StockCounts stockCount
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
                        stockCount
                                .getWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "Stock count belongs to another warehouse"
            );
        }
    }


    // =====================================================
    // OWNER
    // =====================================================

    public void validateOwner(
            Users staff,
            StockCounts stockCount
    ) {

        if (stockCount.getAssignedTo() == null) {

            throw new Forbidden(
                    "Stock count has not been claimed"
            );
        }


        if (!stockCount
                .getAssignedTo()
                .getId()
                .equals(
                        staff.getId()
                )) {

            throw new Forbidden(
                    "Stock count is being handled by another staff"
            );
        }
    }


    // =====================================================
    // EDITABLE STATUS
    // =====================================================

    public void validateEditable(
            StockCounts stockCount
    ) {

        if (stockCount.getStatus()
                != StockCountStatus.COUNTING

                &&

                stockCount.getStatus()
                        != StockCountStatus.RECOUNTING) {

            throw new BadRequest(
                    "Stock count is not editable"
            );
        }
    }


    // =====================================================
    // ITEM
    // =====================================================

    public StockCountItems getItem(
            Long stockCountId,
            String barcode,
            String locationCode
    ) {

        return stockCountItemRepository
                .findItemForCounting(
                        stockCountId,
                        barcode,
                        locationCode
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Product or location does not belong to stock count scope"
                        )
                );
    }
}