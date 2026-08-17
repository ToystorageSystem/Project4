package com.toystorage.backend.dto.request.transfers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PickTransferItemRequest {

    @NotBlank(message = "Product barcode is required")
    private String productBarcode;

    @NotBlank(message = "Location code is required")
    private String locationCode;

    @NotNull(message = "Picked quantity is required")
    @Min(value = 1)
    private Integer quantity;
}