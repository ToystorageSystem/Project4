package com.toystorage.backend.dto.request.stores.returns;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InspectStoreReturnItemRequest {

    @NotNull(message = "Received quantity is required")
    @Min(value = 0, message = "Received quantity cannot be negative")
    private Integer receivedQuantity;

    @NotNull(message = "Condition status is required")
    private String conditionStatus;

    private String note;
}