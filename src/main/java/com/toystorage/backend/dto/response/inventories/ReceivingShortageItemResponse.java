package com.toystorage.backend.dto.response.inventories;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceivingShortageItemResponse {

    private Long productId;

    private String productCode;
    private String productName;
    private String barcode;

    private Integer expectedQuantity;
    private Integer actualQuantity;

    private Integer shortageQuantity;
}