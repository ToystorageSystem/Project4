package com.toystorage.backend.dto.response.transfers;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferPickingItemResponse {

    private Long itemId;

    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;

    private Integer approvedQuantity;
    private Integer pickedQuantity;
    private Integer remainingQuantity;
    private Integer shortageQuantity;

    private String locationCode;
    private String zone;
    private String shelf;
}