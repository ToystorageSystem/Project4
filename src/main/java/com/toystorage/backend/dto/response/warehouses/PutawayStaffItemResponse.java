package com.toystorage.backend.dto.response.warehouses;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PutawayStaffItemResponse {

    private Long itemId;

    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;

    private Integer expectedQuantity;
    private Integer putawayQuantity;
    private Integer remainingQuantity;

    private Long fromLocationId;
    private String fromLocationCode;

    private Long toLocationId;
    private String toLocationCode;

    private String zone;
    private String shelf;

    private String status;
}