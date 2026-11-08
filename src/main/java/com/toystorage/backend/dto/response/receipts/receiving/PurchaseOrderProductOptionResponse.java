package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderProductOptionResponse {

    private Long supplierProductLinkId;

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;

    private String supplierProductCode;

    private BigDecimal purchasePrice;

    private Integer leadTimeDays;

    private Integer minimumOrderQuantity;

    private Boolean defaultSupplier;
}