package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingDetailResponse {

    private Long receiptId;
    private String receiptCode;
    private String status;

    private Long warehouseId;
    private String warehouseName;

    private Long purchaseOrderId;
    private String purchaseOrderCode;

    private Long supplierId;
    private String supplierName;

    private Long receivedBy;
    private String receivedByName;

    private Integer totalExpectedQuantity;
    private Integer totalActualQuantity;

    private Integer totalAcceptedQuantity;
    private Integer totalDamagedQuantity;

    private Integer totalShortageQuantity;
    private Integer totalSurplusQuantity;

    private Integer totalItems;
    private Integer inspectedItems;

    private List<ReceivingItemResponse> items;
}