package com.toystorage.backend.dto.response.receipts.store;

/** One product of one transfer after inspection */
public record ReceivingResultItemResponse(
        String transferCode,
        Long productId,
        String productCode,
        String name,
        int expected,
        int actual,
        int damaged,
        int shortage,
        int surplus) {
}