package com.toystorage.backend.dto.response.stores;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate date,
        long todaySales, long todayItemsSold, BigDecimal todayRevenue,
        long lowShelfCount, long outOfStockCount,
        long manifestsInTransit, long manifestsToReceive,
        long manifestsToInspect, long manifestsToConfirm,
        List<SaleSummary> recentSales) {
}
