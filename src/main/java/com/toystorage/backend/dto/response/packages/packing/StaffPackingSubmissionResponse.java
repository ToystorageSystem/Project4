package com.toystorage.backend.dto.response.packages.packing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class StaffPackingSubmissionResponse {

    /** Filled after submit: trip created for the driver (auto dispatch). */
    private String shipmentCode;

    private String manifestCode;

    /** CREATED = waiting for a free driver, ASSIGNED = driver chosen. */
    private String deliveryStatus;

    private String driverName;

    private Long transferId;

    private String transferCode;

    private String status;

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;

    private Integer totalPickedQuantity;

    private Integer totalPackedQuantity;

    private Integer totalPackages;

    private String completedByName;

    private LocalDateTime completedAt;

    private List<StaffPackingSubmissionPackageResponse> packages;
}