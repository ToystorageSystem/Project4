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
public class StockTransferItemResponse {

    private Long id;

    private String itemCode;

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private String baseUnit;

    private Integer requestedQuantity;

    private Integer approvedQuantity;

    private Integer pickedQuantity;

    private Integer packedQuantity;

    private Integer shippedQuantity;

    private Integer receivedQuantity;

    private Integer shortageQuantity;

    private Integer surplusQuantity;
}