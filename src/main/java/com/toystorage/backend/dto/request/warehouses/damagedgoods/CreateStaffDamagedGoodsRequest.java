package com.toystorage.backend.dto.request.warehouses.damagedgoods;

import com.toystorage.backend.enums.warehouses.DamageType;
import com.toystorage.backend.enums.warehouses.DamagedGoodsSourceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStaffDamagedGoodsRequest {

    @NotNull(
            message = "Product ID is required"
    )
    private Long productId;


    @NotNull(
            message = "Location ID is required"
    )
    private Long locationId;


    @NotNull(
            message = "Affected quantity is required"
    )
    @Min(
            value = 1,
            message = "Affected quantity must be greater than 0"
    )
    private Integer quantity;


    @NotNull(
            message = "Damage type is required"
    )
    private DamageType damageType;


    @NotNull(
            message = "Source type is required"
    )
    private DamagedGoodsSourceType sourceType;


    /*
     * ID chứng từ nguồn.
     *
     * GOODS_RECEIPT -> goods_receipt.id
     * STOCK_TRANSFER -> stock_transfer.id
     * STOCK_COUNT -> stock_count.id
     *
     * MANUAL có thể null.
     */
    private Long sourceId;


    @NotBlank(
            message = "Description is required"
    )
    private String description;


    private String conditionNote;
}