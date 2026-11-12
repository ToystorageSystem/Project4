package com.toystorage.backend.dto.request.stores;

import com.toystorage.backend.enums.stores.MoveDirection;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MoveStockRequest(
        @NotNull(message = "direction is required") MoveDirection direction,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be > 0")
        @Max(value = 100000, message = "quantity is too large") Integer quantity,
        @Size(max = 255, message = "note must be at most 255 characters") String note) {
}