package com.toystorage.backend.dto.response.deliveries.assignment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryAssignmentResponse {

    private Long deliveryId;

    private String shipmentCode;

    private String deliveryStatus;


    // DRIVER

    private Long driverId;

    private String driverCode;

    private String driverName;

    private String driverEmail;


    // ASSIGNMENT

    private Long assignmentHistoryId;

    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime rejectedAt;

    private String rejectionReason;


    // ROUTE

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;


    // EXPECTED

    private LocalDateTime expectedPickupAt;

    private LocalDateTime expectedDeliveryAt;
}