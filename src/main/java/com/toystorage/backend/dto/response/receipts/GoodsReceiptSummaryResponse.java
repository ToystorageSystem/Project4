package com.toystorage.backend.dto.response.receipts;

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
public class GoodsReceiptSummaryResponse {

    private Long id;

    private String receiptCode;

    private String status;

    private Long supplierId;

    private String supplierName;

    private Long warehouseId;

    private String warehouseName;

    private Integer totalProducts;

    private Integer totalExpectedQuantity;

    private LocalDateTime createdAt;
}