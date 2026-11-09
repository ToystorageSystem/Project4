package com.toystorage.backend.dto.response.stores;

import java.time.LocalDateTime;

import com.toystorage.backend.enums.stores.StockTransactionType;

public record StockTransactionLine(Long id, String code, StockTransactionType type,
                                   Long productId, String productCode, String productName,
                                   int shelfChange, int backroomChange,
                                   int shelfAfter, int backroomAfter,
                                   Long saleId, Long storeReceiptId, String note,
                                   String createdByName, LocalDateTime createdAt) {
}
