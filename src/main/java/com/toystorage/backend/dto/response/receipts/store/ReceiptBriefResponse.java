package com.toystorage.backend.dto.response.receipts.store;

import java.time.LocalDateTime;

import com.toystorage.backend.enums.receipts.StoreReceiptStatus;

public record ReceiptBriefResponse(
        Long receiptId,
        String receiptCode,
        String transferCode,
        StoreReceiptStatus status,
        LocalDateTime receivedAt) {     // set by "complete receiving"
}