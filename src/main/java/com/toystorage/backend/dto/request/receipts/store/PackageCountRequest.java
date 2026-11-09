package com.toystorage.backend.dto.request.receipts.store;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** F2 – PUT /packages/{packageCode}/items (all products of the package in ONE request) */
public record PackageCountRequest(
        @NotEmpty(message = "Lines are required")
        List<@Valid Line> lines) {

    public record Line(
            @NotBlank(message = "Barcode is required")
            String barcode,

            @NotNull(message = "Actual quantity is required")
            @Min(value = 0, message = "Actual quantity must be >= 0")
            Integer actualQuantity,

            @NotNull(message = "Damaged quantity is required")
            @Min(value = 0, message = "Damaged quantity must be >= 0")
            Integer damagedQuantity,

            @Size(max = 500, message = "Note is too long")
            String note) {
    }
}