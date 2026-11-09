package com.toystorage.backend.dto.response.packages.packing;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PackingConfirmationResponse {

    private Long transferId;
    private String transferCode;
    private String transferStatus;

    private Integer expectedQuantity;
    private Integer packedQuantity;

    private Boolean quantityMatched;
    private Boolean allPackagesSealed;
    private Boolean allPackagesPacked;

    private Long confirmedBy;
    private String confirmedByName;

    private Long manifestId;
    private String manifestCode;

    private List<PackingPackageResponse> packages;
}