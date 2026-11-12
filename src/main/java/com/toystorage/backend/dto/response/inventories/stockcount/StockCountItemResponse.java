package com.toystorage.backend.dto.response.inventories.stockcount;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockCountItemResponse {

    private Long itemId;

    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;

    private Long locationId;
    private String locationCode;

    private Integer systemQuantity;
    private Integer countedQuantity;
    private Integer differenceQuantity;

    private String result;

    private Long countedBy;
    private String countedByName;
}