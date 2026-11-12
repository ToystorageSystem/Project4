package com.toystorage.backend.dto.response.deliveries.trips;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DeliveryTripDetailResponse {

    private Long deliveryId;

    private String shipmentCode;

    private String status;


    // DRIVER

    private Long driverId;

    private String driverName;


    // FROM

    private Long fromLocationId;

    private String fromLocationCode;

    private String fromLocationName;

    private String fromLocationType;

    private String fromAddress;


    // TO

    private Long toLocationId;

    private String toLocationCode;

    private String toLocationName;

    private String toLocationType;

    private String toAddress;


    // SCHEDULE

    private LocalDateTime expectedPickupAt;

    private LocalDateTime expectedDeliveryAt;


    // ACCEPT / REJECT

    private LocalDateTime acceptedAt;

    private LocalDateTime rejectedAt;

    private String rejectionReason;


    // MANIFEST

    private Long manifestId;

    private String manifestCode;

    private String manifestStatus;


    // PACKAGE

    private Integer packageCount;

    private List<DeliveryTripPackageResponse> packages;
}