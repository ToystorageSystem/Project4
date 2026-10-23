package com.toystorage.backend.dto.response.receipts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptDetailResponse {

    private Long id;

    private String receiptCode;

    private String status;

    private Long purchaseOrderId;

    private String purchaseOrderCode;

    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private Long createdById;

    private String createdByName;

    private String note;

    private BigDecimal estimatedTotalAmount;

    private LocalDateTime createdAt;

    private List<GoodsReceiptItemDetailResponse> items;
}