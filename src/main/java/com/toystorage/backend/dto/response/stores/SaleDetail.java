package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleDetail(Long id, String code, int totalQuantity, BigDecimal totalAmount,
                         String note, String createdByName, LocalDateTime createdAt,
                         List<SaleItemLine> items) {
}