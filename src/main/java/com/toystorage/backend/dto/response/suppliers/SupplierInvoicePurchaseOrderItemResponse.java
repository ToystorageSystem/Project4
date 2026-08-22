package com.toystorage.backend.dto.response.suppliers;

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
public class SupplierInvoicePurchaseOrderItemResponse {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private Integer orderedQuantity;

    private Integer receivedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal lineTotal;
}