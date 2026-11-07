package com.toystorage.backend.dto.response.receipts;

import java.time.LocalDateTime;

// DTO chỉ gồm các trường cần thiết để hiển thị nhật ký hủy PO.
public record PurchaseOrderCancellationHistoryResponse(
        Long id,
        Long cancelledById,
        String cancelledByName,
        LocalDateTime cancelledAt
) {}
