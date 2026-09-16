package com.toystorage.backend.dto.response.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentConfirmationResponse {

    // TRANSFER

    private Long transferId;

    private String transferCode;

    private String transferStatus;


    // WAREHOUSE

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;


    // MANIFEST

    private Long manifestId;

    private String manifestCode;

    private String manifestStatus;


    // DELIVERY

    private Long deliveryId;

    private String shipmentCode;

    private String deliveryStatus;


    // DRIVER

    private Long driverId;

    private String driverName;


    // HANDOVER

    private Long handedOverBy;

    private String handedOverByName;

    private LocalDateTime handedOverAt;


    // PACKAGES

    private List<ShipmentPackageResponse> packages;
}