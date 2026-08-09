package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptInspectionRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String packageCode;

    @NotNull(message = "Expected quantity is required")
    @Min(value = 0, message = "Expected quantity must be greater than or equal to 0")
    private Integer expectedQuantity;

    @NotNull(message = "Actual quantity is required")
    @Min(value = 0, message = "Actual quantity must be greater than or equal to 0")
    private Integer actualQuantity;

    private String notes;
}