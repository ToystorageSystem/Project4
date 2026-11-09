package com.toystorage.backend.dto.response.receipts.store;

import java.time.LocalDateTime;

import com.toystorage.backend.enums.receipts.StoreReceivingStatus;

/** GET /manifests (list row) – also the header of ManifestDetailResponse */
public record ManifestSummaryResponse(
        Long manifestId,
        String manifestCode,
        String shipmentCode,
        String deliveryStatus,          // IN_TRANSIT | ARRIVED | DELIVERED
        String fromWarehouseName,
        LocalDateTime expectedDeliveryAt,
        int totalPackages,
        int receivedPackages,           // RECEIVED + CHECKED
        int checkedPackages,            // CHECKED
        StoreReceivingStatus receivingStatus,
        boolean canReceive) {           // delivery ARRIVED/DELIVERED and receiving not completed
}