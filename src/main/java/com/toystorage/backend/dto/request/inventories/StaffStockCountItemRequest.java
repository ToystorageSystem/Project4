package com.toystorage.backend.dto.request.inventories;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffStockCountItemRequest {

    @NotBlank(
            message = "Location code is required"
    )
    private String locationCode;


    @NotBlank(
            message = "Product barcode is required"
    )
    private String productBarcode;


    @NotNull(
            message = "Count quantity is required"
    )
    @Min(
            value = 0,
            message = "Count quantity cannot be negative"
    )
    private Integer quantity;
}