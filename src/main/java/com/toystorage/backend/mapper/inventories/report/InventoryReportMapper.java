package com.toystorage.backend.mapper.inventories.report;

import com.toystorage.backend.dto.response.inventories.report.InventoryProductLocationResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportItemResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportOptionResponse;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.InventoryStockStatus;
import org.springframework.stereotype.Component;

@Component
public class InventoryReportMapper {

    // =====================================================
    // INVENTORY REPORT ITEM
    // =====================================================

    public InventoryReportItemResponse toReportItem(
            InventoryBalances balance
    ) {

        if (balance == null) {
            return null;
        }

        Products product =
                balance.getProduct();

        Categories category =
                product != null
                        ? product.getCategory()
                        : null;

        Brands brand =
                product != null
                        ? product.getBrand()
                        : null;

        Warehouses warehouse =
                balance.getWarehouse();

        WarehouseLocations location =
                balance.getLocation();

        int onHand =
                valueOrZero(
                        balance.getQuantity()
                );

        int reserved =
                valueOrZero(
                        balance.getReservedQuantity()
                );

        int available =
                calculateAvailable(
                        onHand,
                        reserved
                );

        int minimumStock =
                valueOrZero(
                        balance.getMinimumStockLevel()
                );

        InventoryStockStatus stockStatus =
                determineStockStatus(
                        available,
                        minimumStock
                );

        return InventoryReportItemResponse.builder()

                // =========================
                // INVENTORY BALANCE
                // =========================

                .inventoryBalanceId(
                        balance.getId()
                )

                // =========================
                // WAREHOUSE / STORE
                // =========================

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

                .warehouseType(
                        warehouse != null
                                && warehouse.getType() != null
                                ? warehouse.getType().name()
                                : null
                )

                // =========================
                // LOCATION
                // =========================

                .locationId(
                        location != null
                                ? location.getId()
                                : null
                )

                .locationCode(
                        location != null
                                ? location.getWarehouseCode()
                                : null
                )

                .locationName(
                        location != null
                                ? location.getName()
                                : null
                )

                .zone(
                        location != null
                                ? location.getZone()
                                : null
                )

                .shelf(
                        location != null
                                ? location.getShelf()
                                : null
                )

                // =========================
                // PRODUCT
                // =========================

                .productId(
                        product != null
                                ? product.getId()
                                : null
                )

                .productCode(
                        product != null
                                ? product.getProductsCode()
                                : null
                )

                .productName(
                        product != null
                                ? product.getName()
                                : null
                )

                .barcode(
                        product != null
                                ? product.getBarcode()
                                : null
                )

                .baseUnit(
                        product != null
                                ? product.getBaseUnit()
                                : null
                )

                // =========================
                // CATEGORY
                // =========================

                .categoryId(
                        category != null
                                ? category.getId()
                                : null
                )

                .categoryCode(
                        category != null
                                ? category.getCategoriesCode()
                                : null
                )

                .categoryName(
                        category != null
                                ? category.getName()
                                : null
                )

                // =========================
                // BRAND
                // =========================

                .brandId(
                        brand != null
                                ? brand.getId()
                                : null
                )

                .brandCode(
                        brand != null
                                ? brand.getBrandsCode()
                                : null
                )

                .brandName(
                        brand != null
                                ? brand.getName()
                                : null
                )

                // =========================
                // INVENTORY
                // =========================

                .onHandQuantity(
                        onHand
                )

                .reservedQuantity(
                        reserved
                )

                .availableQuantity(
                        available
                )

                .minimumStockLevel(
                        minimumStock
                )

                .stockStatus(
                        stockStatus.name()
                )

                .lastUpdatedAt(
                        balance.getUpdatedAt()
                )

                .build();
    }


    // =====================================================
    // PRODUCT LOCATION DETAIL
    // =====================================================

    public InventoryProductLocationResponse toProductLocation(
            InventoryBalances balance
    ) {

        if (balance == null) {
            return null;
        }

        Products product =
                balance.getProduct();

        Warehouses warehouse =
                balance.getWarehouse();

        WarehouseLocations location =
                balance.getLocation();

        int onHand =
                valueOrZero(
                        balance.getQuantity()
                );

        int reserved =
                valueOrZero(
                        balance.getReservedQuantity()
                );

        int available =
                calculateAvailable(
                        onHand,
                        reserved
                );

        int minimumStock =
                valueOrZero(
                        balance.getMinimumStockLevel()
                );

        InventoryStockStatus stockStatus =
                determineStockStatus(
                        available,
                        minimumStock
                );

        return InventoryProductLocationResponse.builder()

                // =========================
                // PRODUCT
                // =========================

                .productId(
                        product != null
                                ? product.getId()
                                : null
                )

                .productCode(
                        product != null
                                ? product.getProductsCode()
                                : null
                )

                .productName(
                        product != null
                                ? product.getName()
                                : null
                )

                .barcode(
                        product != null
                                ? product.getBarcode()
                                : null
                )

                .baseUnit(
                        product != null
                                ? product.getBaseUnit()
                                : null
                )

                // =========================
                // WAREHOUSE / STORE
                // =========================

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

                .warehouseType(
                        warehouse != null
                                && warehouse.getType() != null
                                ? warehouse.getType().name()
                                : null
                )

                // =========================
                // LOCATION
                // =========================

                .locationId(
                        location != null
                                ? location.getId()
                                : null
                )

                .locationCode(
                        location != null
                                ? location.getWarehouseCode()
                                : null
                )

                .locationName(
                        location != null
                                ? location.getName()
                                : null
                )

                .zone(
                        location != null
                                ? location.getZone()
                                : null
                )

                .shelf(
                        location != null
                                ? location.getShelf()
                                : null
                )

                // =========================
                // INVENTORY
                // =========================

                .onHandQuantity(
                        onHand
                )

                .reservedQuantity(
                        reserved
                )

                .availableQuantity(
                        available
                )

                .minimumStockLevel(
                        minimumStock
                )

                .stockStatus(
                        stockStatus.name()
                )

                .lastUpdatedAt(
                        balance.getUpdatedAt()
                )

                .build();
    }


    // =====================================================
    // WAREHOUSE OPTION
    // =====================================================

    public InventoryReportOptionResponse toWarehouseOption(
            Warehouses warehouse
    ) {

        if (warehouse == null) {
            return null;
        }

        return InventoryReportOptionResponse.builder()

                .id(
                        warehouse.getId()
                )

                .code(
                        warehouse.getWarehousesCode()
                )

                .name(
                        warehouse.getName()
                )

                .type(
                        warehouse.getType() != null
                                ? warehouse.getType().name()
                                : null
                )

                .build();
    }


    // =====================================================
    // CATEGORY OPTION
    // =====================================================

    public InventoryReportOptionResponse toCategoryOption(
            Categories category
    ) {

        if (category == null) {
            return null;
        }

        return InventoryReportOptionResponse.builder()

                .id(
                        category.getId()
                )

                .code(
                        category.getCategoriesCode()
                )

                .name(
                        category.getName()
                )

                .type(null)

                .build();
    }


    // =====================================================
    // BRAND OPTION
    // =====================================================

    public InventoryReportOptionResponse toBrandOption(
            Brands brand
    ) {

        if (brand == null) {
            return null;
        }

        return InventoryReportOptionResponse.builder()

                .id(
                        brand.getId()
                )

                .code(
                        brand.getBrandsCode()
                )

                .name(
                        brand.getName()
                )

                .type(null)

                .build();
    }


    // =====================================================
    // STOCK STATUS
    // =====================================================

    public InventoryStockStatus determineStockStatus(
            Integer availableQuantity,
            Integer minimumStockLevel
    ) {

        int available =
                valueOrZero(
                        availableQuantity
                );

        int minimum =
                valueOrZero(
                        minimumStockLevel
                );

        if (available <= 0) {
            return InventoryStockStatus.OUT_OF_STOCK;
        }

        if (available <= minimum) {
            return InventoryStockStatus.LOW_STOCK;
        }

        return InventoryStockStatus.IN_STOCK;
    }


    // =====================================================
    // HELPERS
    // =====================================================

    private int calculateAvailable(
            Integer quantity,
            Integer reservedQuantity
    ) {

        int onHand =
                valueOrZero(
                        quantity
                );

        int reserved =
                valueOrZero(
                        reservedQuantity
                );

        return Math.max(
                onHand - reserved,
                0
        );
    }

    private int valueOrZero(
            Integer value
    ) {

        return value != null
                ? value
                : 0;
    }
}