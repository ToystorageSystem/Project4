package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummary(Long id, String code, int totalQuantity, BigDecimal totalAmount,
                          String createdByName, LocalDateTime createdAt) {
}