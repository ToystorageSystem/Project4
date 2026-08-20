package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

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

    private Integer totalProducts;

    private Integer inspectedProducts;

    private Integer remainingProducts;

    private Integer totalExpectedQuantity;

    private Integer totalActualQuantity;

    private Integer totalDamagedQuantity;

    private Integer totalShortageQuantity;

    private Integer totalSurplusQuantity;

    private Double progressPercent;

    private List<ReceivingIssueResponse> issues;
}