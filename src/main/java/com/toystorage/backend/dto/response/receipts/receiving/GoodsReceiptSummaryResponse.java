package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptSummaryResponse {

    private Long id;

    private String receiptCode;

    private Long purchaseOrderId;
    private String purchaseOrderCode;
    private Long purchaseOrderCreatedById;
    private String purchaseOrderCreatedByName;

    private String status;

    private Long supplierId;

    private String supplierName;

    private Long warehouseId;

    private String warehouseName;

    private Integer totalProducts;

    private Integer totalExpectedQuantity;

    private LocalDateTime createdAt;
}