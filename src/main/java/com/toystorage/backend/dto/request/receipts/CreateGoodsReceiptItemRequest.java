package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGoodsReceiptItemRequest {

    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be greater than 0")
    private Long productId;

    @NotNull(message = "Expected quantity is required")
    @Min(value = 1, message = "Expected quantity must be at least 1")
    private Integer expectedQuantity;
}