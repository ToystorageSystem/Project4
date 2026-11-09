package com.toystorage.backend.repository.stores;

import java.math.BigDecimal;

/** count / sum of sales in a time range. SUM returns null when there is no row -> use the safe getters. */
public record SalesTotals(Long salesCount, Long itemsSold, BigDecimal revenue) {

    public long safeSalesCount() {
        return salesCount == null ? 0 : salesCount;
    }

    public long safeItemsSold() {
        return itemsSold == null ? 0 : itemsSold;
    }

    public BigDecimal safeRevenue() {
        return revenue == null ? BigDecimal.ZERO : revenue;
    }
}