package com.toystorage.backend.dto.request.warehouses;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutePutawayItemRequest {

    @NotBlank(message = "Product barcode is required")
    private String productBarcode;

    @NotBlank(message = "Location code is required")
    private String locationCode;

    @NotNull(message = "Putaway quantity is required")
    @Min(value = 1, message = "Putaway quantity must be greater than 0")
    private Integer quantity;
}