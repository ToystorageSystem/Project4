package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.SalesDashboardResponse;
import com.toystorage.backend.dto.response.analytics.SalesTrendPointResponse;
import com.toystorage.backend.enums.stores.SalesOrderStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.analytics.SalesAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesAnalyticsService {

    private final SalesAnalyticsRepository salesAnalyticsRepository;


    // =========================================================
    // SALES SUMMARY
    // =========================================================

    public SalesDashboardResponse getSalesSummary(
            Long storeId,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );

        Long totalOrders =
                safeLong(
                        salesAnalyticsRepository.countOrders(
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        Long draftOrders =
                safeLong(
                        salesAnalyticsRepository.countOrdersByStatus(
                                SalesOrderStatus.DRAFT,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        Long pendingPaymentOrders =
                safeLong(
                        salesAnalyticsRepository.countOrdersByStatus(
                                SalesOrderStatus.PENDING_PAYMENT,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        Long completedOrders =
                safeLong(
                        salesAnalyticsRepository.countOrdersByStatus(
                                SalesOrderStatus.COMPLETED,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        Long cancelledOrders =
                safeLong(
                        salesAnalyticsRepository.countOrdersByStatus(
                                SalesOrderStatus.CANCELLED,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        Long refundedOrders =
                safeLong(
                        salesAnalyticsRepository.countOrdersByStatus(
                                SalesOrderStatus.REFUNDED,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        BigDecimal completedRevenue =
                safeBigDecimal(
                        salesAnalyticsRepository.sumOrderAmountByStatus(
                                SalesOrderStatus.COMPLETED,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        Long completedSoldQuantity =
                safeLong(
                        salesAnalyticsRepository.sumSoldQuantityByStatus(
                                SalesOrderStatus.COMPLETED,
                                storeId,
                                fromDate,
                                toDate
                        )
                );

        BigDecimal averageCompletedOrderValue =
                calculateAverageOrderValue(
                        completedRevenue,
                        completedOrders
                );

        return SalesDashboardResponse.builder()
                .storeId(storeId)
                .fromDate(fromDate)
                .toDate(toDate)
                .totalOrders(totalOrders)
                .draftOrders(draftOrders)
                .pendingPaymentOrders(pendingPaymentOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .refundedOrders(refundedOrders)
                .completedRevenue(completedRevenue)
                .completedSoldQuantity(completedSoldQuantity)
                .averageCompletedOrderValue(
                        averageCompletedOrderValue
                )
                .build();
    }


    // =========================================================
    // DAILY SALES TREND
    // =========================================================

    public List<SalesTrendPointResponse> getDailySalesTrend(
            Long storeId,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );

        List<Object[]> orderRows =
                salesAnalyticsRepository.findDailyOrderTrend(
                        SalesOrderStatus.COMPLETED,
                        storeId,
                        fromDate,
                        toDate
                );

        List<Object[]> quantityRows =
                salesAnalyticsRepository.findDailySoldQuantityTrend(
                        SalesOrderStatus.COMPLETED,
                        storeId,
                        fromDate,
                        toDate
                );

        return mergeTrendRows(
                orderRows,
                quantityRows
        );
    }


    // =========================================================
    // MONTHLY SALES TREND
    // =========================================================

    public List<SalesTrendPointResponse> getMonthlySalesTrend(
            Long storeId,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );

        List<Object[]> orderRows =
                salesAnalyticsRepository.findMonthlyOrderTrend(
                        SalesOrderStatus.COMPLETED,
                        storeId,
                        fromDate,
                        toDate
                );

        List<Object[]> quantityRows =
                salesAnalyticsRepository.findMonthlySoldQuantityTrend(
                        SalesOrderStatus.COMPLETED,
                        storeId,
                        fromDate,
                        toDate
                );

        return mergeTrendRows(
                orderRows,
                quantityRows
        );
    }


    // =========================================================
    // MERGE TREND RESULT
    //
    // orderRows:
    //
    // [0] period
    // [1] revenue
    // [2] completedOrders
    //
    // quantityRows:
    //
    // [0] period
    // [1] soldQuantity
    // =========================================================

    private List<SalesTrendPointResponse> mergeTrendRows(
            List<Object[]> orderRows,
            List<Object[]> quantityRows
    ) {

        Map<String, Long> soldQuantityByPeriod =
                new HashMap<>();

        if (quantityRows != null) {

            for (Object[] row : quantityRows) {

                if (
                        row == null
                                || row.length < 2
                                || row[0] == null
                ) {
                    continue;
                }

                String period =
                        row[0].toString();

                Long soldQuantity =
                        toLong(
                                row[1]
                        );

                soldQuantityByPeriod.put(
                        period,
                        soldQuantity
                );
            }
        }

        List<SalesTrendPointResponse> result =
                new ArrayList<>();

        if (orderRows == null) {
            return result;
        }

        for (Object[] row : orderRows) {

            if (
                    row == null
                            || row.length < 3
                            || row[0] == null
            ) {
                continue;
            }

            String period =
                    row[0].toString();

            BigDecimal revenue =
                    toBigDecimal(
                            row[1]
                    );

            Long completedOrders =
                    toLong(
                            row[2]
                    );

            Long soldQuantity =
                    soldQuantityByPeriod.getOrDefault(
                            period,
                            0L
                    );

            result.add(
                    SalesTrendPointResponse.builder()
                            .period(period)
                            .revenue(revenue)
                            .completedOrders(completedOrders)
                            .soldQuantity(soldQuantity)
                            .build()
            );
        }

        return result;
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateDateRange(
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(toDate)
        ) {

            throw new BadRequest(
                    "Thời gian bắt đầu không được sau thời gian kết thúc"
            );
        }
    }


    // =========================================================
    // SAFE CONVERSION
    // =========================================================

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }


    private BigDecimal safeBigDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    private Long toLong(
            Object value
    ) {

        if (value == null) {
            return 0L;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(
                value.toString()
        );
    }


    private BigDecimal toBigDecimal(
            Object value
    ) {

        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(
                value.toString()
        );
    }


    // =========================================================
    // AVERAGE ORDER VALUE
    // =========================================================

    private BigDecimal calculateAverageOrderValue(
            BigDecimal revenue,
            Long completedOrders
    ) {

        if (
                completedOrders == null
                        || completedOrders == 0
        ) {

            return BigDecimal.ZERO;
        }

        return revenue.divide(
                BigDecimal.valueOf(
                        completedOrders
                ),
                2,
                RoundingMode.HALF_UP
        );
    }
}