package com.toystorage.backend.dto.response.transfers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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