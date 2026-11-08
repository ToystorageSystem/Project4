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
public class InventoryTransferRecommendationResponse {

    // =========================================================
    // PRODUCT
    // =========================================================

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;


    // =========================================================
    // SOURCE
    // =========================================================

    private Long sourceWarehouseId;

    private String sourceWarehouseCode;

    private String sourceWarehouseName;

    private String sourceWarehouseType;

    private Long sourceAvailableQuantity;

    private Long sourceOutboundQuantity;


    // =========================================================
    // TARGET
    // =========================================================

    private Long targetWarehouseId;

    private String targetWarehouseCode;

    private String targetWarehouseName;

    private String targetWarehouseType;

    private Long targetAvailableQuantity;

    private Long targetOutboundQuantity;


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    /**
     * Số lượng hệ thống đề xuất điều chuyển.
     *
     * Đây chỉ là recommendation.
     * Không tự động tạo Stock Transfer.
     */
    private Long suggestedQuantity;


    /**
     * Tồn khả dụng dự kiến tại source sau khi chuyển.
     */
    private Long sourceAvailableAfterTransfer;


    /**
     * Tồn khả dụng dự kiến tại target sau khi nhận.
     */
    private Long targetAvailableAfterTransfer;


    /**
     * Ví dụ:
     *
     * REDISTRIBUTE_TO_ACTIVE_STORE
     */
    private String recommendationType;


    /**
     * Giải thích vì sao hệ thống đưa ra đề xuất.
     */
    private String reason;
}