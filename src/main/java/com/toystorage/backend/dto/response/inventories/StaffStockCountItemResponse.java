package com.toystorage.backend.dto.response.inventories;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffStockCountItemResponse {

    private Long itemId;

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;


    private Long locationId;

    private String locationCode;

    private String zone;

    private String shelf;


    private Integer systemQuantity;

    private Integer firstCountQuantity;

    private Integer secondCountQuantity;

    private Integer finalQuantity;

    private Integer differenceQuantity;


    private String countedByName;

    private String recountedByName;
}