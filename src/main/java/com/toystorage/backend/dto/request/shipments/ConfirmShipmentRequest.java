package com.toystorage.backend.dto.request.shipments;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmShipmentRequest {

    @NotNull(message = "Delivery id is required")
    private Long deliveryId;
}