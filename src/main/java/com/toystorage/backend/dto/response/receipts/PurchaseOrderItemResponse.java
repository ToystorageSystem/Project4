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
public class PurchaseOrderItemResponse {

    private Long id;

    private String itemCode;

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;

    private Integer orderedQuantity;

    private Integer receivedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal lineTotal;
}