package com.toystorage.backend.dto.response.warehouses.putaway;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PutawayPlanItemResponse {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    private Integer expectedQuantity;

    private Integer putawayQuantity;

    private String status;

    private Long fromLocationId;

    private String fromLocationCode;

    private String fromLocationName;

    private Long toLocationId;

    private String toLocationCode;

    private String toLocationName;

    private String zone;

    private String shelf;
}