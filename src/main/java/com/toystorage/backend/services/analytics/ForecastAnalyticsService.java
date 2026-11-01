package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.ForecastAnalyticsResponse;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.analytics.ForecastAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForecastAnalyticsService {

    private final ForecastAnalyticsRepository forecastAnalyticsRepository;


    // =========================================================
    // TECHNICAL BASELINE
    //
    // Ít nhất phải có 2 ngày sales khác nhau mới cho phép
    // baseline forecast.
    //
    // Đây KHÔNG phải business threshold từ BA.
    // Đây chỉ là điều kiện kỹ thuật tối thiểu để tránh forecast
    // từ đúng một điểm dữ liệu.
    // =========================================================

    private static final long MIN_OBSERVED_SALES_DAYS = 2L;

    private static final int DEFAULT_FORECAST_PERIOD_DAYS = 7;


    // =========================================================
    // FORECAST ANALYTICS
    // =========================================================

    public List<ForecastAnalyticsResponse> getForecastAnalytics(

            Long storeId,

            Long productId,

            LocalDateTime fromDate,

            LocalDateTime toDate,

            Integer forecastPeriodDays
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        int effectiveForecastPeriodDays =
                validateAndResolveForecastPeriod(
                        forecastPeriodDays
                );


        List<ForecastAnalyticsRepository.ForecastBaseRow> rows =
                forecastAnalyticsRepository.findForecastBaseData(
                        storeId,
                        productId,
                        fromDate,
                        toDate
                );


        if (
                rows == null
                        || rows.isEmpty()
        ) {

            return new ArrayList<>();
        }


        List<ForecastAnalyticsResponse> result =
                new ArrayList<>();


        for (
                ForecastAnalyticsRepository.ForecastBaseRow row
                : rows
        ) {

            result.add(
                    buildResponse(
                            row,
                            effectiveForecastPeriodDays
                    )
            );
        }


        return result;
    }


    // =========================================================
    // BUILD RESPONSE
    // =========================================================

    private ForecastAnalyticsResponse buildResponse(

            ForecastAnalyticsRepository.ForecastBaseRow row,

            int forecastPeriodDays
    ) {

        long historyCalendarDays =
                safeLong(
                        row.getHistoryCalendarDays()
                );


        long observedSalesDays =
                safeLong(
                        row.getObservedSalesDays()
                );


        long totalSoldQuantity =
                safeLong(
                        row.getTotalSoldQuantity()
                );


        long availableQuantity =
                safeLong(
                        row.getAvailableQuantity()
                );


        // =====================================================
        // DESCRIPTIVE SALES METRIC
        //
        // Đây chỉ là average lịch sử.
        // KHÔNG tự gọi nó là forecast.
        // =====================================================

        BigDecimal averageDailySoldQuantity =
                calculateAverageDailySoldQuantity(
                        totalSoldQuantity,
                        historyCalendarDays
                );


        // =====================================================
        // HISTORY SUFFICIENCY
        //
        // Một điểm dữ liệu không đủ để forecast.
        // =====================================================

        boolean sufficientHistory =
                historyCalendarDays >= MIN_OBSERVED_SALES_DAYS
                        && observedSalesDays
                        >= MIN_OBSERVED_SALES_DAYS;


        String forecastStatus;

        String forecastMethod;

        BigDecimal forecastQuantity = null;

        BigDecimal projectedAvailableAfterForecast = null;

        String stockRiskStatus;


        if (
                !sufficientHistory
        ) {

            forecastStatus =
                    "INSUFFICIENT_HISTORY";

            forecastMethod =
                    "NONE";

            stockRiskStatus =
                    "INSUFFICIENT_HISTORY";

        } else {

            // =================================================
            // SIMPLE BASELINE FORECAST
            //
            // averageDailySoldQuantity × forecastPeriodDays
            //
            // Đây là thống kê baseline.
            //
            // KHÔNG phải AI model.
            // KHÔNG phải ML model.
            // =================================================

            forecastStatus =
                    "BASELINE_AVAILABLE";

            forecastMethod =
                    "SIMPLE_DAILY_AVERAGE";


            forecastQuantity =
                    averageDailySoldQuantity

                            .multiply(
                                    BigDecimal.valueOf(
                                            forecastPeriodDays
                                    )
                            )

                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );


            projectedAvailableAfterForecast =
                    BigDecimal
                            .valueOf(
                                    availableQuantity
                            )

                            .subtract(
                                    forecastQuantity
                            )

                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );


            stockRiskStatus =
                    determineStockRiskStatus(
                            projectedAvailableAfterForecast
                    );
        }


        // =====================================================
        // STORED ANALYTICS
        //
        // Tách hoàn toàn khỏi forecast realtime.
        //
        // Priority:
        //
        // DEMAND_FORECAST
        // SALES_FORECAST
        // STOCK_RISK
        // INVENTORY_TREND
        // =====================================================

        ForecastAnalyticsRepository.StoredAnalyticsRow
                storedAnalytics =
                findStoredAnalytics(
                        row.getProductId(),
                        row.getStoreId()
                );


        // =====================================================
        // RECOMMENDATION
        // =====================================================

        String recommendation =
                buildRecommendation(
                        sufficientHistory,
                        forecastPeriodDays,
                        forecastQuantity,
                        availableQuantity,
                        projectedAvailableAfterForecast,
                        stockRiskStatus
                );


        // =====================================================
        // RESPONSE
        // =====================================================

        ForecastAnalyticsResponse.ForecastAnalyticsResponseBuilder builder =
                ForecastAnalyticsResponse
                        .builder()

                        // =========================================
                        // PRODUCT
                        // =========================================

                        .productId(
                                row.getProductId()
                        )

                        .productCode(
                                row.getProductCode()
                        )

                        .productName(
                                row.getProductName()
                        )

                        .baseUnit(
                                row.getBaseUnit()
                        )


                        // =========================================
                        // STORE
                        // =========================================

                        .storeId(
                                row.getStoreId()
                        )

                        .storeCode(
                                row.getStoreCode()
                        )

                        .storeName(
                                row.getStoreName()
                        )


                        // =========================================
                        // INVENTORY
                        // =========================================

                        .currentQuantity(
                                safeLong(
                                        row.getCurrentQuantity()
                                )
                        )

                        .reservedQuantity(
                                safeLong(
                                        row.getReservedQuantity()
                                )
                        )

                        .availableQuantity(
                                availableQuantity
                        )


                        // =========================================
                        // SALES HISTORY
                        // =========================================

                        .historyStartDate(
                                row.getHistoryStartDate()
                        )

                        .historyEndDate(
                                row.getHistoryEndDate()
                        )

                        .historyCalendarDays(
                                historyCalendarDays
                        )

                        .observedSalesDays(
                                observedSalesDays
                        )

                        .completedOrders(
                                safeLong(
                                        row.getCompletedOrders()
                                )
                        )

                        .totalSoldQuantity(
                                totalSoldQuantity
                        )

                        .totalRevenue(
                                safeBigDecimal(
                                        row.getTotalRevenue()
                                )
                        )

                        .averageDailySoldQuantity(
                                averageDailySoldQuantity
                        )


                        // =========================================
                        // FORECAST CONFIG
                        // =========================================

                        .forecastPeriodDays(
                                forecastPeriodDays
                        )


                        // =========================================
                        // SUFFICIENCY
                        // =========================================

                        .sufficientHistory(
                                sufficientHistory
                        )

                        .forecastStatus(
                                forecastStatus
                        )

                        .forecastMethod(
                                forecastMethod
                        )


                        // =========================================
                        // FORECAST
                        // =========================================

                        .forecastQuantity(
                                forecastQuantity
                        )

                        .projectedAvailableAfterForecast(
                                projectedAvailableAfterForecast
                        )


                        // =========================================
                        // STOCK RISK
                        // =========================================

                        .stockRiskStatus(
                                stockRiskStatus
                        )


                        // =========================================
                        // RECOMMENDATION
                        // =========================================

                        .recommendation(
                                recommendation
                        );


        // =====================================================
        // STORED ANALYTICS RESULT
        // =====================================================

        if (
                storedAnalytics != null
        ) {

            builder
                    .storedAnalyticsResultId(
                            storedAnalytics
                                    .getStoredAnalyticsResultId()
                    )

                    .storedAnalyticsCode(
                            storedAnalytics
                                    .getStoredAnalyticsCode()
                    )

                    .storedAnalysisType(
                            storedAnalytics
                                    .getStoredAnalysisType()
                    )

                    .storedGeneratedAt(
                            storedAnalytics
                                    .getStoredGeneratedAt()
                    )

                    .storedResultData(
                            storedAnalytics
                                    .getStoredResultData()
                    )

                    .storedScore(
                            storedAnalytics
                                    .getStoredScore()
                    );
        }


        return builder.build();
    }


    // =========================================================
    // FIND STORED ANALYTICS
    //
    // Dữ liệu này CHỈ để tham khảo lịch sử đã lưu.
    //
    // Nó KHÔNG tham gia công thức forecast realtime.
    // =========================================================

    private ForecastAnalyticsRepository.StoredAnalyticsRow
    findStoredAnalytics(

            Long productId,

            Long storeId
    ) {

        if (
                productId == null
                        || storeId == null
        ) {

            return null;
        }


        String[] preferredAnalysisTypes = {

                "DEMAND_FORECAST",

                "SALES_FORECAST",

                "STOCK_RISK",

                "INVENTORY_TREND"
        };


        for (
                String analysisType
                : preferredAnalysisTypes
        ) {

            List<ForecastAnalyticsRepository.StoredAnalyticsRow>
                    storedRows =
                    forecastAnalyticsRepository
                            .findLatestStoredAnalytics(
                                    productId,
                                    storeId,
                                    analysisType
                            );


            if (
                    storedRows != null
                            && !storedRows.isEmpty()
            ) {

                return storedRows.get(0);
            }
        }


        return null;
    }


    // =========================================================
    // AVERAGE DAILY SOLD
    //
    // totalSold / historyCalendarDays
    //
    // Chỉ là descriptive metric.
    // =========================================================

    private BigDecimal calculateAverageDailySoldQuantity(

            long totalSoldQuantity,

            long historyCalendarDays
    ) {

        if (
                historyCalendarDays <= 0L
        ) {

            return BigDecimal.ZERO;
        }


        return BigDecimal
                .valueOf(
                        totalSoldQuantity
                )

                .divide(
                        BigDecimal.valueOf(
                                historyCalendarDays
                        ),
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // STOCK RISK
    //
    // Không đặt threshold tùy ý.
    //
    // Chỉ so sánh trực tiếp:
    //
    // available - forecast
    // =========================================================

    private String determineStockRiskStatus(

            BigDecimal projectedAvailableAfterForecast
    ) {

        if (
                projectedAvailableAfterForecast == null
        ) {

            return "INSUFFICIENT_HISTORY";
        }


        int comparison =
                projectedAvailableAfterForecast
                        .compareTo(
                                BigDecimal.ZERO
                        );


        if (
                comparison < 0
        ) {

            return "STOCKOUT_RISK";
        }


        if (
                comparison == 0
        ) {

            return "STOCK_DEPLETION_RISK";
        }


        return "SUFFICIENT_STOCK";
    }


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    private String buildRecommendation(

            boolean sufficientHistory,

            int forecastPeriodDays,

            BigDecimal forecastQuantity,

            long availableQuantity,

            BigDecimal projectedAvailableAfterForecast,

            String stockRiskStatus
    ) {

        if (
                !sufficientHistory
        ) {

            return "Chưa đủ lịch sử bán hàng để tạo dự báo đáng tin cậy. "
                    + "Hệ thống chỉ hiển thị dữ liệu bán hàng và tồn kho "
                    + "hiện có; cần thêm dữ liệu bán ở nhiều ngày khác nhau "
                    + "trước khi chạy baseline forecast.";
        }


        if (
                "STOCKOUT_RISK".equals(
                        stockRiskStatus
                )
        ) {

            return "Baseline dự báo khoảng "
                    + formatNumber(
                            forecastQuantity
                    )
                    + " đơn vị trong "
                    + forecastPeriodDays
                    + " ngày, trong khi tồn khả dụng hiện tại là "
                    + availableQuantity
                    + ". Nếu nhu cầu tiếp tục theo baseline này, "
                    + "tồn kho có nguy cơ không đủ. "
                    + "Cần xem xét bổ sung hoặc điều chuyển hàng.";
        }


        if (
                "STOCK_DEPLETION_RISK".equals(
                        stockRiskStatus
                )
        ) {

            return "Baseline dự báo có thể sử dụng hết lượng tồn khả dụng "
                    + "trong "
                    + forecastPeriodDays
                    + " ngày. Cần theo dõi sát tồn kho và kế hoạch bổ sung.";
        }


        return "Baseline dự báo khoảng "
                + formatNumber(
                        forecastQuantity
                )
                + " đơn vị trong "
                + forecastPeriodDays
                + " ngày. Sau dự báo, tồn khả dụng ước tính còn "
                + formatNumber(
                        projectedAvailableAfterForecast
                )
                + " đơn vị.";
    }


    // =========================================================
    // FORECAST PERIOD
    // =========================================================

    private int validateAndResolveForecastPeriod(

            Integer forecastPeriodDays
    ) {

        if (
                forecastPeriodDays == null
        ) {

            return DEFAULT_FORECAST_PERIOD_DAYS;
        }


        if (
                forecastPeriodDays <= 0
        ) {

            throw new BadRequest(
                    "Số ngày dự báo phải lớn hơn 0"
            );
        }


        return forecastPeriodDays;
    }


    // =========================================================
    // DATE VALIDATION
    // =========================================================

    private void validateDateRange(

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(
                                toDate
                        )
        ) {

            throw new BadRequest(
                    "Thời gian bắt đầu không được sau thời gian kết thúc"
            );
        }
    }


    // =========================================================
    // FORMAT
    // =========================================================

    private String formatNumber(
            BigDecimal value
    ) {

        if (
                value == null
        ) {

            return "0";
        }


        return value
                .stripTrailingZeros()
                .toPlainString();
    }


    // =========================================================
    // SAFE CONVERSION
    // =========================================================

    private long safeLong(
            Long value
    ) {

        return value == null
                ? 0L
                : value;
    }


    private BigDecimal safeBigDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}