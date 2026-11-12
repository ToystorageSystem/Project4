package com.toystorage.backend.dto.response.deliveries.trips;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryTripListResponse {

    private Long deliveryId;

    private String shipmentCode;

    private String status;


    private Long fromLocationId;

    private String fromLocationCode;

    private String fromLocationName;

    private String fromLocationType;

    private String fromAddress;


    private Long toLocationId;

    private String toLocationCode;

    private String toLocationName;

    private String toLocationType;

    private String toAddress;


    private LocalDateTime expectedPickupAt;

    private LocalDateTime expectedDeliveryAt;


    private Integer packageCount;
}