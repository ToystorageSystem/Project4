package com.toystorage.backend.dto.request.stores;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MinShelfRequest(
        @NotNull(message = "minShelfQuantity is required")
        @Min(value = 0, message = "minShelfQuantity must be >= 0")
        @Max(value = 100000, message = "minShelfQuantity is too large") Integer minShelfQuantity) {
}