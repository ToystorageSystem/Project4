package com.toystorage.backend.dto.request.stores.returns;

import com.toystorage.backend.enums.stores.ReturnItemCondition;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InspectReturnedItemRequest {

    @NotBlank(
            message = "Product barcode is required"
    )
    private String productBarcode;


    @NotNull(
            message = "Received quantity is required"
    )
    @Min(
            value = 0,
            message = "Received quantity cannot be negative"
    )
    private Integer receivedQuantity;


    @NotNull(
            message = "Condition status is required"
    )
    private ReturnItemCondition conditionStatus;


    private String note;
}