package com.toystorage.backend.dto.response.receipts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderSummaryResponse {

    private Long id;

    private String orderCode;

    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    private String status;

    private LocalDate expectedDeliveryDate;

    private Integer totalProducts;

    private Integer totalOrderedQuantity;

    private BigDecimal totalAmount;

    private Long createdById;

    private String createdByCode;

    private String createdByName;

    private Long approvedById;

    private String approvedByCode;

    private String approvedByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}