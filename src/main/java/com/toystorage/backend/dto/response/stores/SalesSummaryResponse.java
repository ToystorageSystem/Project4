package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesSummaryResponse(LocalDate from, LocalDate to,
                                   long salesCount, long itemsSold, BigDecimal revenue) {
}
