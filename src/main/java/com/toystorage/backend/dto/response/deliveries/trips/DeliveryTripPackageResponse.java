package com.toystorage.backend.dto.response.deliveries.trips;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliveryTripPackageResponse {

    private Long packageId;

    private String packageCode;

    private String sealNumber;

    private String status;
}