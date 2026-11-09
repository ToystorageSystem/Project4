package com.toystorage.backend.dto.response.receipts.store;

import java.util.List;

import com.toystorage.backend.enums.receipts.PackageReceivingState;

/** GET / PUT /packages/{packageCode}/items */
public record PackageCheckDetailResponse(
        Long packageId,
        String packageCode,
        String manifestCode,
        PackageReceivingState status,
        List<PackageCheckLineResponse> lines) {
}