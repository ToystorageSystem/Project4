package com.toystorage.backend.dto.request.stores;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleLineRequest(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be > 0")
        @Max(value = 1000, message = "quantity is too large") Integer quantity) {
}