package com.toystorage.backend.dto.request.packages;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanPackageItemRequest {

    @NotBlank(
            message = "Product barcode is required"
    )
    private String productBarcode;

    /*
     * Scan thông thường frontend gửi quantity = 1.
     *
     * Bulk packing có thể gửi quantity > 1.
     */
    @NotNull(
            message = "Quantity is required"
    )
    @Min(
            value = 1,
            message = "Quantity must be greater than 0"
    )
    private Integer quantity;
}