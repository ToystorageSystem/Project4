package com.toystorage.backend.dto.response.receipts.store;

import java.util.List;

import com.toystorage.backend.enums.receipts.StoreReceivingStatus;

/** POST finish-inspection / confirm */
public record ReceivingResultResponse(
        String manifestCode,
        StoreReceivingStatus receivingStatus,
        List<ReceivingResultItemResponse> items,
        int totalExpected,
        int totalActual,
        int totalDamaged,
        int totalShortage,
        int totalSurplus,
        List<String> discrepancyReportCodes) {   // filled by confirm (empty before)
}