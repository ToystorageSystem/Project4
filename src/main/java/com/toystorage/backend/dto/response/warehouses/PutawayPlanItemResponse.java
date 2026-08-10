package com.toystorage.backend.dto.response.warehouses;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PutawayPlanItemResponse {

    private Long id;

    private Long productId;
    private String productName;

    private Integer expectedQuantity;
    private Integer putawayQuantity;

    private String status;

    private Long fromLocationId;
    private String fromLocationName;

    private Long toLocationId;
    private String toLocationName;

    private String zone;
    private String shelf;
}