package com.toystorage.backend.dto.response.deliveries.trips;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliveryTripActionResponse {

    private Long deliveryId;

    private String message;

    private String status;
}