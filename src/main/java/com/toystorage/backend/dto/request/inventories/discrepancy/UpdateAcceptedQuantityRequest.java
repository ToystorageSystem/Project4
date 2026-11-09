package com.toystorage.backend.dto.request.inventories.discrepancy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAcceptedQuantityRequest {

    @NotNull(message = "Accepted quantity is required")
    @Min(value = 0, message = "Accepted quantity cannot be negative")
    private Integer acceptedQuantity;

    @NotBlank(message = "Reason is required")
    private String reason;
}