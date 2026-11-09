package com.toystorage.backend.dto.response.transfers.delivery;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferProductOptionResponse {

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private String baseUnit;

    private Integer availableQuantity;
}