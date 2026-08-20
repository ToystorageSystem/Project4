package com.toystorage.backend.dto.response.packages.packing;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StaffPackingSubmissionResponse {

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