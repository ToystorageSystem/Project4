package com.toystorage.backend.dto.response.packages.packing;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffPackingSubmissionPackageResponse {

    private Long packageId;

    private String packageCode;

    private String packageStatus;

    private String sealNumber;

    private Integer totalQuantity;
}