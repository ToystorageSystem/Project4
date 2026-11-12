package com.toystorage.backend.dto.response.warehouses.damagedgoods;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DamagedGoodsItemResponse {

    private Long itemId;

    private Long productId;
    private String productName;

    private Integer quantity;

    private String damageType;
    private String conditionNote;

    private Long locationId;
    private String locationName;

    private String disposition;
    private String status;
}