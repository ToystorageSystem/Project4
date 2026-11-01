package com.toystorage.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastAnalyticsResponse {

    // =========================================================
    // PRODUCT
    // =========================================================

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;


    // =========================================================
    // STORE
    // =========================================================

    private Long storeId;

    private String storeCode;

    private String storeName;


    // =========================================================
    // CURRENT INVENTORY
    // =========================================================

    private Long currentQuantity;

    private Long reservedQuantity;

    /**
     * quantity - reservedQuantity
     *
     * Không phụ thuộc hoàn toàn vào available_quantity lưu sẵn,
     * tránh trường hợp dữ liệu stored available bị stale.
     */
    private Long availableQuantity;


    // =========================================================
    // SALES HISTORY
    // =========================================================

    private LocalDate historyStartDate;

    private LocalDate historyEndDate;

    /**
     * Số ngày calendar từ ngày đầu đến ngày cuối của dữ liệu.
     */
    private Long historyCalendarDays;

    /**
     * Số ngày thực sự có completed sales.
     */
    private Long observedSalesDays;

    private Long completedOrders;

    private Long totalSoldQuantity;

    private BigDecimal totalRevenue;

    /**
     * Chỉ số mô tả lịch sử:
     *
     * totalSoldQuantity / historyCalendarDays
     *
     * Đây KHÔNG phải forecast.
     */
    private BigDecimal averageDailySoldQuantity;


    // =========================================================
    // FORECAST CONFIGURATION
    // =========================================================

    /**
     * Khoảng thời gian muốn dự báo.
     *
     * Ví dụ:
     * 7 ngày.
     */
    private Integer forecastPeriodDays;


    // =========================================================
    // DATA SUFFICIENCY
    // =========================================================

    /**
     * true:
     * đủ dữ liệu để chạy baseline forecast.
     *
     * false:
     * dữ liệu lịch sử chưa đủ.
     */
    private Boolean sufficientHistory;


    /**
     * Ví dụ:
     *
     * INSUFFICIENT_HISTORY
     * BASELINE_AVAILABLE
     */
    private String forecastStatus;


    /**
     * Ví dụ:
     *
     * NONE
     * SIMPLE_DAILY_AVERAGE
     *
     * Không gọi là AI/ML nếu thực tế chỉ là công thức thống kê.
     */
    private String forecastMethod;


    // =========================================================
    // FORECAST RESULT
    // =========================================================

    /**
     * Forecast được tính từ dữ liệu sales thật.
     *
     * null nếu history chưa đủ.
     */
    private BigDecimal forecastQuantity;


    /**
     * availableQuantity - forecastQuantity
     *
     * null nếu chưa có forecast.
     */
    private BigDecimal projectedAvailableAfterForecast;


    // =========================================================
    // STOCK RISK
    // =========================================================

    /**
     * Ví dụ:
     *
     * INSUFFICIENT_HISTORY
     * STOCKOUT_RISK
     * SUFFICIENT_STOCK
     */
    private String stockRiskStatus;


    // =========================================================
    // EXISTING STORED ANALYTICS RESULT
    //
    // analytics_results hiện đã có dữ liệu mẫu cũ.
    //
    // Ta trả riêng để phân biệt:
    //
    // - forecast vừa tính từ sales thật
    // - analytics result đã lưu trước đó
    // =========================================================

    private Long storedAnalyticsResultId;

    private String storedAnalyticsCode;

    private String storedAnalysisType;

    private LocalDateTime storedGeneratedAt;

    private String storedResultData;

    private BigDecimal storedScore;


    // =========================================================
    // ANALYTICS
    // =========================================================

    private String recommendation;
}