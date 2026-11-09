package com.toystorage.backend.dto.request.inventories.discrepancy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessManagerResolveDiscrepancyItemRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Final quantity is required")
    @PositiveOrZero(message = "Final quantity must be greater than or equal to 0")
    private Integer finalQuantity;
}