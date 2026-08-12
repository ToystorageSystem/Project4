package com.toystorage.backend.dto.response.shipments;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentConfirmationResponse {

    private Long transferId;
    private String transferCode;
    private String transferStatus;

    private Long manifestId;
    private String manifestCode;
    private String manifestStatus;

    private Long deliveryId;
    private String shipmentCode;
    private String deliveryStatus;

    private Long driverId;
    private String driverName;

    private Long handedOverBy;
    private String handedOverByName;

    private LocalDateTime handedOverAt;

    private List<ShipmentPackageResponse> packages;
}