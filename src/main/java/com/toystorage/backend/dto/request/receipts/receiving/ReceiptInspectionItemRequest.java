package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptInspectionItemRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Actual quantity is required")
    @Min(
            value = 0,
            message = "Actual quantity cannot be negative"
    )
    private Integer actualQuantity;

    @Min(
            value = 0,
            message = "Damaged quantity cannot be negative"
    )
    private Integer damagedQuantity = 0;

    private String packageCode;

    private String notes;
}