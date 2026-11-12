package com.toystorage.backend.dto.response.packages.packing;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PackingPackageResponse {

    private Long packageId;
    private String packageCode;

    private String status;

    private String sealNumber;
    private Boolean sealed;

    private Long packedBy;
    private String packedByName;

    private Long checkedBy;
    private String checkedByName;

    private Integer totalQuantity;

    private List<PackingPackageItemResponse> items;
}