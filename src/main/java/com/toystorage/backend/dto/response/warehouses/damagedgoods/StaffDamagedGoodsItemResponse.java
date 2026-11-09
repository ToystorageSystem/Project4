package com.toystorage.backend.dto.response.warehouses.damagedgoods;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffDamagedGoodsItemResponse {

    private Long itemId;

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;


    private Long locationId;

    private String locationCode;

    private String locationName;


    private Integer quantity;

    private String damageType;

    private String conditionNote;

    private String evidenceImageUrl;

    private String disposition;

    private String status;
}