package com.toystorage.backend.dto.response.inventories.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryProductLocationResponse {

    // =========================
    // PRODUCT
    // =========================

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private String baseUnit;

    // =========================
    // WAREHOUSE / STORE
    // =========================

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String warehouseType;

    // =========================
    // LOCATION
    // =========================

    private Long locationId;

    private String locationCode;

    private String locationName;

    private String zone;

    private String shelf;

    // =========================
    // INVENTORY
    // =========================

    private Integer onHandQuantity;

    private Integer reservedQuantity;

    private Integer availableQuantity;

    private Integer minimumStockLevel;

    private String stockStatus;

    private LocalDateTime lastUpdatedAt;
}