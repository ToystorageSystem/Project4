package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingMonitorResponse {

    private Long receiptId;

    private String receiptCode;

    private String status;

    private Long warehouseId;

    private Long staffId;

    private String staffName;

    // Product progress
    private Integer totalProducts;

    private Integer inspectedProducts;

    private Integer remainingProducts;

    private Integer issueProducts;

    // Quantity progress
    private Integer totalExpectedQuantity;

    private Integer totalActualQuantity;

    private Integer remainingQuantity;

    private Double progressPercent;

    private LocalDateTime receivingStartedAt;
}