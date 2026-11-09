package com.toystorage.backend.dto.request.deliveries.handover;

import com.toystorage.backend.enums.deliveries.HandoverPackageCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanHandoverPackageRequest {

    @NotBlank(message = "Package code is required")
    private String packageCode;

    private String actualSealNumber;

    @NotNull(message = "Package condition is required")
    private HandoverPackageCondition condition;

    private String issueNote;
}