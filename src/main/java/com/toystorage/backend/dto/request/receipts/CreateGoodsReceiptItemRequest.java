package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateGoodsReceiptItemRequest {

    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be greater than 0")
    private Long productId;

    @NotNull(message = "Expected quantity is required")
    @Min(
            value = 1,
            message = "Expected quantity must be at least 1"
    )
    private Integer expectedQuantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(
            value = "0.01",
            message = "Unit price must be greater than 0"
    )
    private BigDecimal unitPrice;
}