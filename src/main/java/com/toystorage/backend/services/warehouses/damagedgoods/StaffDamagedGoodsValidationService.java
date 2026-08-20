package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;

import com.toystorage.backend.entity.inventories.InventoryBalances;

import com.toystorage.backend.entity.products.Products;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsReportRepository;

import com.toystorage.backend.repository.inventories.damagedgoods.StaffDamagedGoodsInventoryRepository;

import com.toystorage.backend.repository.products.ProductRepository;

import com.toystorage.backend.repository.users.UserRepository;

import com.toystorage.backend.repository.warehouses.putaway.WarehouseLocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffDamagedGoodsValidationService {

    private final UserRepository
            userRepository;

    private final ProductRepository
            productRepository;

    private final WarehouseLocationRepository
            warehouseLocationRepository;

    private final DamagedGoodsReportRepository
            reportRepository;

    private final StaffDamagedGoodsInventoryRepository
            inventoryRepository;


    // =====================================================
    // USER
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
    // WAREHOUSE
    // =====================================================

    public Long getWarehouseId(
            Users staff
    ) {

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }


        return staff
                .getWarehouse()
                .getId();
    }


    // =====================================================
    // PRODUCT
    // =====================================================

    public Products getProduct(
            Long productId
    ) {

        return productRepository
                .findById(productId)

                .orElseThrow(() ->
                        new NotFound(
                                "Product not found"
                        )
                );
    }


    // =====================================================
    // LOCATION
    // =====================================================

    public WarehouseLocations getLocation(
            Long locationId,
            Long warehouseId
    ) {

        WarehouseLocations location =
                warehouseLocationRepository
                        .findById(locationId)

                        .orElseThrow(() ->
                                new NotFound(
                                        "Warehouse location not found"
                                )
                        );


        if (!location
                .getWarehouse()
                .getId()
                .equals(warehouseId)) {

            throw new Forbidden(
                    "Location belongs to another warehouse"
            );
        }


        return location;
    }


    // =====================================================
    // INVENTORY
    // =====================================================

    public InventoryBalances validateQuantity(
            Long warehouseId,
            Long locationId,
            Long productId,
            Integer damagedQuantity
    ) {

        if (damagedQuantity == null
                || damagedQuantity <= 0) {

            throw new BadRequest(
                    "Damaged quantity must be greater than 0"
            );
        }


        InventoryBalances balance =
                inventoryRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                warehouseId,
                                locationId,
                                productId
                        )

                        .orElseThrow(() ->
                                new BadRequest(
                                        "Product has no inventory at selected location"
                                )
                        );


        /*
         * Rule:
         *
         * Số lượng báo hỏng không được vượt
         * số lượng vật lý ở location.
         */
        if (damagedQuantity
                > balance.getQuantity()) {

            throw new BadRequest(
                    "Damaged quantity exceeds physical quantity at location. "
                            + "Current quantity: "
                            + balance.getQuantity()
            );
        }


        return balance;
    }


    // =====================================================
    // REPORT OWNER
    // =====================================================

    public DamagedGoodsReports getMyReport(
            Long reportId,
            Users staff
    ) {

        DamagedGoodsReports report =
                reportRepository
                        .findById(reportId)

                        .orElseThrow(() ->
                                new NotFound(
                                        "Damaged goods report not found"
                                )
                        );


        if (report.getReportedBy() == null
                || !report
                .getReportedBy()
                .getId()
                .equals(staff.getId())) {

            throw new Forbidden(
                    "You cannot access another staff's report"
            );
        }


        return report;
    }
}
