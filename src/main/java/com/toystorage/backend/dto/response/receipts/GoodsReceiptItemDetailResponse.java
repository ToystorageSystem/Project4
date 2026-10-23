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
public class GoodsReceiptItemDetailResponse {

    private Long productId;
    private String productCode;
    private String productName;

    private Integer expectedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}