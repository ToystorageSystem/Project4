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
public class InventoryReportItemResponse {

    private Long inventoryBalanceId;

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
    // PRODUCT
    // =========================

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private String baseUnit;

    // =========================
    // CATEGORY
    // =========================

    private Long categoryId;

    private String categoryCode;

    private String categoryName;

    // =========================
    // BRAND
    // =========================

    private Long brandId;

    private String brandCode;

    private String brandName;

    // =========================
    // INVENTORY
    // =========================

    /**
     * Tồn thực tế / on-hand.
     */
    private Integer onHandQuantity;

    /**
     * Số lượng đang được giữ.
     */
    private Integer reservedQuantity;

    /**
     * Tồn khả dụng.
     *
     * available = onHand - reserved
     */
    private Integer availableQuantity;

    /**
     * Mức tồn tối thiểu.
     */
    private Integer minimumStockLevel;

    /**
     * IN_STOCK
     * LOW_STOCK
     * OUT_OF_STOCK
     */
    private String stockStatus;

    /**
     * Thời gian cập nhật tồn gần nhất.
     */
    private LocalDateTime lastUpdatedAt;
}