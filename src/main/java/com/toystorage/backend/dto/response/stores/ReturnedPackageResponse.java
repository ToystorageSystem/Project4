package com.toystorage.backend.dto.response.stores;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnedPackageResponse {

    private Long packageId;

    private String packageCode;

    private String sealNumber;

    private String status;
}