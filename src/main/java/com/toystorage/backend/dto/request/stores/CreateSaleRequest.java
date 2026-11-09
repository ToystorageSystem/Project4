package com.toystorage.backend.dto.request.stores;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSaleRequest(
        @NotEmpty(message = "items is required")
        @Size(max = 50, message = "A sale can have at most 50 lines")
        List<@NotNull(message = "item must not be null") @Valid SaleLineRequest> items,
        @Size(max = 500, message = "note must be at most 500 characters") String note) {
}