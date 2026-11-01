package com.toystorage.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAnomalyResponse {

    // =========================================================
    // INVENTORY BALANCE / LOCATION
    // =========================================================

    private Long inventoryBalanceId;

    private Long locationId;


    // =========================================================
    // WAREHOUSE
    // =========================================================

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String warehouseType;


    // =========================================================
    // PRODUCT
    // =========================================================

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;


    // =========================================================
    // INVENTORY
    // =========================================================

    private Long currentQuantity;

    private Long reservedQuantity;

    private Long calculatedAvailableQuantity;

    private Long storedAvailableQuantity;


    // =========================================================
    // ANOMALY
    // =========================================================

    private String anomalyType;

    private String severity;

    private String description;

    private String recommendation;
}