package com.toystorage.backend.dto.response.packages.shipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchHandoverListResponse {

    private Long transferId;

    private String transferCode;

    private Long manifestId;

    private String manifestCode;

    private String fromWarehouseName;

    private String toWarehouseName;

    private Long deliveryId;

    private String shipmentCode;

    private String deliveryStatus;

    private Long driverId;

    private String driverName;

    private Integer packageCount;
}