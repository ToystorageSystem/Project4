package com.toystorage.backend.dto.response.receipts.store;

import com.toystorage.backend.enums.receipts.PackageCondition;
import com.toystorage.backend.enums.receipts.PackageReceivingState;
import com.toystorage.backend.enums.receipts.SealStatus;

/** One package of a manifest (F1 result shown here; null = not received yet) */
public record PackageStatusResponse(
        Long packageId,
        String packageCode,
        String sealNumber,              // expected seal (written by the warehouse)
        String transferCode,
        PackageReceivingState status,
        SealStatus sealStatus,
        PackageCondition packageCondition,
        String notes,
        int lineCount,                  // number of products in the package
        int pieces) {                   // total quantity in the package
}