package com.toystorage.backend.dto.response.shipments;

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
public class ShipmentPackageItemResponse {

    // PRODUCT
    private Long productId;

    private String productCode;

    private String productName;


    // UNIT
    private String unit;


    // QUANTITY
    private Integer quantity;
}