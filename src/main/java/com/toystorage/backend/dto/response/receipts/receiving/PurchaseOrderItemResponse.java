package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

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