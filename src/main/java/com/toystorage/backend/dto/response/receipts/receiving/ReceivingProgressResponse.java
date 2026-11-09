package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingProgressResponse {

    private Long receiptId;

    private String receiptCode;

    private String status;

    private Long staffId;

    private String staffName;

    /*
     * Thời điểm Warehouse Staff
     * claim GOODS_RECEIVING.
     */
    private LocalDateTime receivingStartedAt;

    /*
     * Thời điểm Warehouse Staff
     * finish inspection và release task.
     *
     * null = vẫn đang kiểm.
     */
    private LocalDateTime inspectionCompletedAt;

    private Integer totalProducts;

    private Integer inspectedProducts;

    private Integer remainingProducts;

    private Integer totalExpectedQuantity;

    private Integer totalActualQuantity;

    private Integer totalDamagedQuantity;

    private Integer totalShortageQuantity;

    private Integer totalSurplusQuantity;

    private Double progressPercent;

    /*
     * Toàn bộ product trong receipt.
     */
    private List<ReceivingProductProgressResponse> products;

    /*
     * Product có vấn đề.
     */
    private List<ReceivingIssueResponse> issues;
}