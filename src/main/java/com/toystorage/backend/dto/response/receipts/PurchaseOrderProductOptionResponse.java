package com.toystorage.backend.dto.response.receipts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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