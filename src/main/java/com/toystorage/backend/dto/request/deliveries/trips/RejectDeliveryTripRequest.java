package com.toystorage.backend.dto.request.deliveries.trips;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectDeliveryTripRequest {

    @NotBlank(
            message = "Rejection reason is required"
    )
    @Size(
            max = 500,
            message = "Rejection reason must not exceed 500 characters"
    )
    private String reason;
}