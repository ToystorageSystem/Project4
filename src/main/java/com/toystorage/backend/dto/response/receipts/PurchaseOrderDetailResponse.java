package com.toystorage.backend.dto.response.receipts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDetailResponse {

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

    private String note;

    private String rejectionReason;

    private String cancelReason;

    private Integer totalProducts;

    private Integer totalOrderedQuantity;

    private BigDecimal totalAmount;

    private Long createdById;

    private String createdByCode;

    private String createdByName;

    private Long approvedById;

    private String approvedByCode;

    private String approvedByName;

    private LocalDateTime approvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<PurchaseOrderItemResponse> items;
}