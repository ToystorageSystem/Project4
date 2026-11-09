package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItemDetailResponse {

    private Long productId;
    private String productCode;
    private String productName;

    private Integer expectedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}