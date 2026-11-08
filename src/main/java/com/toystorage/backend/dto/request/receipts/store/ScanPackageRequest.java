package com.toystorage.backend.dto.request.receipts.store;

import com.toystorage.backend.enums.receipts.PackageCondition;
import com.toystorage.backend.enums.receipts.SealStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** F1 – POST /manifests/{manifestCode}/packages/scan */
public record ScanPackageRequest(
        @NotBlank(message = "Package code is required")
        @Size(max = 100, message = "Package code is too long")
        String packageCode,

        @NotNull(message = "Seal status is required")
        SealStatus sealStatus,               // INTACT | BROKEN | MISSING

        @NotNull(message = "Package condition is required")
        PackageCondition packageCondition,   // GOOD | DENTED | TORN | WET | DAMAGED

        @Size(max = 1000, message = "Notes are too long")
        String notes) {
}