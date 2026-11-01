package com.toystorage.backend.dto.response.analytics;

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
public class SlowMovingInventoryResponse {

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String warehouseType;

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;

    private Long currentQuantity;

    private Long reservedQuantity;

    private Long availableQuantity;

    /**
     * Tổng số lượng xuất khỏi tồn kho
     * trong khoảng thời gian phân tích.
     */
    private Long outboundQuantity;

    /**
     * Số giao dịch làm giảm tồn.
     */
    private Long outboundTransactionCount;

    /**
     * Giao dịch giảm tồn gần nhất
     * trong khoảng thời gian phân tích.
     */
    private LocalDateTime lastOutboundAt;

    /**
     * NO_OUTBOUND
     * SLOW_MOVING
     * NORMAL
     * FAST_MOVING
     */
    private String movementType;

    private String recommendation;
}