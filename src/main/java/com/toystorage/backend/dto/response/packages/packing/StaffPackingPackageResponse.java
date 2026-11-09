package com.toystorage.backend.dto.response.packages.packing;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StaffPackingPackageResponse {

    private Long packageId;

    private String packageCode;

    private String status;

    private Long transferId;

    private String transferCode;

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;

    private String sealNumber;

    private Integer totalQuantity;

    private String packedByName;

    private LocalDateTime packedAt;

    private LocalDateTime sealedAt;

    private List<StaffPackingPackageItemResponse> items;
}