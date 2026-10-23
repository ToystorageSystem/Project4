package com.toystorage.backend.dto.request.transfers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStockTransferItemRequest {

    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be greater than 0")
    private Long productId;

    @NotNull(message = "Requested quantity is required")
    @Min(
            value = 1,
            message = "Requested quantity must be greater than 0"
    )
    private Integer requestedQuantity;
}