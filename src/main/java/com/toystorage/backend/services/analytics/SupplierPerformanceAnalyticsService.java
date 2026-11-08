package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.SupplierPerformanceAnalyticsResponse;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.analytics.SupplierPerformanceAnalyticsRepository;

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
public class SupplierPerformanceAnalyticsService {

    private final SupplierPerformanceAnalyticsRepository
            supplierPerformanceAnalyticsRepository;


    // =========================================================
    // SUPPLIER PERFORMANCE ANALYTICS
    // PHÂN TÍCH HIỆU SUẤT NHÀ CUNG CẤP
    //
    // ENGLISH NOTE:
    // Analyze supplier performance from historical delivery
    // data and current purchase-order activity.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Phân tích hiệu suất nhà cung cấp từ lịch sử giao hàng
    // và hoạt động Purchase Order hiện tại.
    //
    // This service is read-only.
    // Service này chỉ đọc dữ liệu, không thay đổi database.
    // =========================================================

    public List<SupplierPerformanceAnalyticsResponse>
    getSupplierPerformanceAnalytics(
            Long supplierId,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        List<SupplierPerformanceAnalyticsRepository
                .SupplierPerformanceAnalyticsRow> rows =
                supplierPerformanceAnalyticsRepository
                        .findSupplierPerformanceAnalytics(
                                supplierId,
                                fromDate,
                                toDate
                        );


        if (
                rows == null
                        || rows.isEmpty()
        ) {

            return new ArrayList<>();
        }


        List<SupplierPerformanceAnalyticsResponse> result =
                new ArrayList<>();


        for (
                SupplierPerformanceAnalyticsRepository
                        .SupplierPerformanceAnalyticsRow row
                : rows
        ) {

            result.add(
                    buildResponse(
                            row
                    )
            );
        }


        return result;
    }


    // =========================================================
    // BUILD RESPONSE
    // TẠO RESPONSE
    // =========================================================

    private SupplierPerformanceAnalyticsResponse buildResponse(
            SupplierPerformanceAnalyticsRepository
                    .SupplierPerformanceAnalyticsRow row
    ) {

        // Historical performance exists when performance code exists.
        // Có lịch sử performance khi supplierPerformanceCode tồn tại.

        boolean performanceHistoryAvailable =
                row.getSupplierPerformanceCode() != null;


        BigDecimal onTimeDeliveryRate =
                calculateOnTimeDeliveryRate(
                        performanceHistoryAvailable,
                        row.getOnTimeDeliveries(),
                        row.getTotalDeliveries()
                );


        BigDecimal receivingRate =
                calculateRate(
                        safeLong(
                                row.getTotalReceivedQuantity()
                        ),
                        safeLong(
                                row.getTotalOrderedQuantity()
                        )
                );


        String supplierAnalysis =
                determineSupplierAnalysis(
                        performanceHistoryAvailable,
                        row
                );


        String recommendation =
                buildRecommendation(
                        performanceHistoryAvailable,
                        supplierAnalysis,
                        row
                );


        return SupplierPerformanceAnalyticsResponse
                .builder()

                // =================================================
                // SUPPLIER
                // NHÀ CUNG CẤP
                // =================================================

                .supplierId(
                        row.getSupplierId()
                )

                .supplierName(
                        row.getSupplierName()
                )

                .supplierStatus(
                        row.getSupplierStatus()
                )


                // =================================================
                // HISTORICAL PERFORMANCE
                // HIỆU SUẤT LỊCH SỬ
                // =================================================

                .performanceHistoryAvailable(
                        performanceHistoryAvailable
                )

                .supplierPerformanceCode(
                        row.getSupplierPerformanceCode()
                )

                .totalDeliveries(
                        row.getTotalDeliveries()
                )

                .onTimeDeliveries(
                        row.getOnTimeDeliveries()
                )

                .shortageCount(
                        row.getShortageCount()
                )

                .damagedCount(
                        row.getDamagedCount()
                )

                .accuracyRate(
                        row.getAccuracyRate()
                )

                .onTimeDeliveryRate(
                        onTimeDeliveryRate
                )

                .evaluatedAt(
                        row.getEvaluatedAt()
                )


                // =================================================
                // PURCHASE ORDERS
                // ĐƠN MUA HÀNG
                // =================================================

                .totalPurchaseOrders(
                        safeLong(
                                row.getTotalPurchaseOrders()
                        )
                )

                .draftOrders(
                        safeLong(
                                row.getDraftOrders()
                        )
                )

                .orderedOrders(
                        safeLong(
                                row.getOrderedOrders()
                        )
                )

                .partiallyReceivedOrders(
                        safeLong(
                                row.getPartiallyReceivedOrders()
                        )
                )

                .completedOrders(
                        safeLong(
                                row.getCompletedOrders()
                        )
                )

                .cancelledOrders(
                        safeLong(
                                row.getCancelledOrders()
                        )
                )


                // =================================================
                // SUPPLIER RESPONSES
                // PHẢN HỒI NHÀ CUNG CẤP
                // =================================================

                .availableResponses(
                        safeLong(
                                row.getAvailableResponses()
                        )
                )

                .partiallyAvailableResponses(
                        safeLong(
                                row.getPartiallyAvailableResponses()
                        )
                )

                .delayedResponses(
                        safeLong(
                                row.getDelayedResponses()
                        )
                )

                .declinedResponses(
                        safeLong(
                                row.getDeclinedResponses()
                        )
                )

                .outOfStockResponses(
                        safeLong(
                                row.getOutOfStockResponses()
                        )
                )

                .discontinuedResponses(
                        safeLong(
                                row.getDiscontinuedResponses()
                        )
                )

                .priceChangedResponses(
                        safeLong(
                                row.getPriceChangedResponses()
                        )
                )

                .pendingResponses(
                        safeLong(
                                row.getPendingResponses()
                        )
                )


                // =================================================
                // DELIVERY / RECEIVING
                // GIAO / NHẬN HÀNG
                // =================================================

                .overdueOrders(
                        safeLong(
                                row.getOverdueOrders()
                        )
                )

                .totalOrderedQuantity(
                        safeLong(
                                row.getTotalOrderedQuantity()
                        )
                )

                .totalReceivedQuantity(
                        safeLong(
                                row.getTotalReceivedQuantity()
                        )
                )

                .receivingRate(
                        receivingRate
                )


                // =================================================
                // PURCHASE VALUE
                // GIÁ TRỊ MUA HÀNG
                // =================================================

                .totalPurchaseAmount(
                        safeBigDecimal(
                                row.getTotalPurchaseAmount()
                        )
                )


                // =================================================
                // ANALYTICS
                // PHÂN TÍCH
                // =================================================

                .supplierAnalysis(
                        supplierAnalysis
                )

                .recommendation(
                        recommendation
                )

                .build();
    }


    // =========================================================
    // SUPPLIER ANALYSIS
    // PHÂN LOẠI TÌNH TRẠNG NHÀ CUNG CẤP
    //
    // ENGLISH NOTE:
    // Do not create arbitrary ratings such as GOOD / BAD.
    // Only report conditions directly supported by data.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Không tự đặt ngưỡng GOOD / BAD.
    // Chỉ phân loại vấn đề chứng minh được từ dữ liệu thật.
    // =========================================================

    private String determineSupplierAnalysis(
            boolean performanceHistoryAvailable,
            SupplierPerformanceAnalyticsRepository
                    .SupplierPerformanceAnalyticsRow row
    ) {

        if (
                safeLong(
                        row.getDeclinedResponses()
                ) > 0L
        ) {

            return "SUPPLIER_DECLINED";
        }


        if (
                safeLong(
                        row.getDiscontinuedResponses()
                ) > 0L
        ) {

            return "PRODUCT_DISCONTINUED";
        }


        if (
                safeLong(
                        row.getOutOfStockResponses()
                ) > 0L
        ) {

            return "SUPPLIER_OUT_OF_STOCK";
        }


        if (
                safeLong(
                        row.getDelayedResponses()
                ) > 0L
        ) {

            return "SUPPLIER_DELAYED";
        }


        if (
                safeLong(
                        row.getOverdueOrders()
                ) > 0L
        ) {

            return "OVERDUE_ORDERS";
        }


        if (
                safeLong(
                        row.getPriceChangedResponses()
                ) > 0L
        ) {

            return "PRICE_CHANGED";
        }


        if (
                safeLong(
                        row.getPartiallyAvailableResponses()
                ) > 0L
        ) {

            return "PARTIALLY_AVAILABLE";
        }


        if (
                safeLong(
                        row.getPendingResponses()
                ) > 0L
        ) {

            return "WAITING_SUPPLIER_RESPONSE";
        }


        if (
                performanceHistoryAvailable
                        && safeLong(
                                row.getShortageCount()
                        ) > 0L
        ) {

            return "HISTORICAL_SHORTAGE";
        }


        if (
                performanceHistoryAvailable
                        && safeLong(
                                row.getDamagedCount()
                        ) > 0L
        ) {

            return "HISTORICAL_DAMAGE";
        }


        if (!performanceHistoryAvailable) {

            return "NO_PERFORMANCE_HISTORY";
        }


        return "NORMAL";
    }


    // =========================================================
    // RECOMMENDATION
    // ĐỀ XUẤT
    //
    // ENGLISH NOTE:
    // All text returned to frontend must be English.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Toàn bộ nội dung trả về frontend phải bằng tiếng Anh.
    // =========================================================

    private String buildRecommendation(
            boolean performanceHistoryAvailable,
            String supplierAnalysis,
            SupplierPerformanceAnalyticsRepository
                    .SupplierPerformanceAnalyticsRow row
    ) {

        return switch (supplierAnalysis) {

            case "SUPPLIER_DECLINED" ->

                    "The supplier has declined at least one purchase order. "
                            + "Review the reason and consider an alternative supply source.";


            case "PRODUCT_DISCONTINUED" ->

                    "The supplier reported that at least one product has been discontinued. "
                            + "Consider finding an alternative product or supplier.";


            case "SUPPLIER_OUT_OF_STOCK" ->

                    "The supplier reported an out-of-stock condition. "
                            + "Review the impact on the purchasing plan "
                            + "and consider an additional supply source.";


            case "SUPPLIER_DELAYED" ->

                    "The supplier reported a delivery delay. "
                            + "Monitor delivery progress and its impact on inventory.";


            case "OVERDUE_ORDERS" -> {

                long overdueOrders =
                        safeLong(
                                row.getOverdueOrders()
                        );


                yield "There "
                        + (overdueOrders == 1L ? "is " : "are ")
                        + overdueOrders
                        + (overdueOrders == 1L
                        ? " purchase order "
                        : " purchase orders ")
                        + "past the expected delivery date and not yet completed. "
                        + "Contact the supplier and monitor receiving progress.";
            }


            case "PRICE_CHANGED" ->

                    "The supplier reported a price change. "
                            + "Review the confirmed price before continuing "
                            + "to process the purchase order.";


            case "PARTIALLY_AVAILABLE" ->

                    "The supplier can fulfill only part of the ordered quantity. "
                            + "Evaluate the shortage and consider an additional supply source.";


            case "WAITING_SUPPLIER_RESPONSE" -> {

                long pendingResponses =
                        safeLong(
                                row.getPendingResponses()
                        );


                yield "There "
                        + (pendingResponses == 1L ? "is " : "are ")
                        + pendingResponses
                        + (pendingResponses == 1L
                        ? " purchase order "
                        : " purchase orders ")
                        + "awaiting supplier response. "
                        + "Follow up with the supplier to confirm availability.";
            }


            case "HISTORICAL_SHORTAGE" -> {

                long shortageCount =
                        safeLong(
                                row.getShortageCount()
                        );


                yield "Historical performance data recorded "
                        + shortageCount
                        + (shortageCount == 1L
                        ? " shortage case. "
                        : " shortage cases. ")
                        + "Continue monitoring delivery accuracy.";
            }


            case "HISTORICAL_DAMAGE" -> {

                long damagedCount =
                        safeLong(
                                row.getDamagedCount()
                        );


                yield "Historical performance data recorded "
                        + damagedCount
                        + (damagedCount == 1L
                        ? " damaged-item case. "
                        : " damaged-item cases. ")
                        + "Continue monitoring packaging and delivery quality.";
            }


            case "NO_PERFORMANCE_HISTORY" ->

                    "The supplier does not yet have historical performance data. "
                            + "Continue monitoring current purchase orders "
                            + "and record performance after receiving data becomes available.";


            default -> {

                if (performanceHistoryAvailable) {

                    yield "No direct supplier performance issue was detected "
                            + "from the current data. Continue monitoring supplier responses, "
                            + "delivery progress, and receiving results.";
                }


                yield "Continue monitoring the supplier's purchase orders "
                        + "and receiving data.";
            }
        };
    }


    // =========================================================
    // ON-TIME DELIVERY RATE
    // TỶ LỆ GIAO HÀNG ĐÚNG HẠN
    // =========================================================

    private BigDecimal calculateOnTimeDeliveryRate(
            boolean performanceHistoryAvailable,
            Long onTimeDeliveries,
            Long totalDeliveries
    ) {

        if (!performanceHistoryAvailable) {

            return null;
        }


        return calculateRate(
                safeLong(
                        onTimeDeliveries
                ),
                safeLong(
                        totalDeliveries
                )
        );
    }


    // =========================================================
    // GENERIC RATE
    // TÍNH TỶ LỆ CHUNG
    // =========================================================

    private BigDecimal calculateRate(
            long numerator,
            long denominator
    ) {

        if (denominator <= 0L) {

            return BigDecimal.ZERO;
        }


        return BigDecimal
                .valueOf(
                        numerator
                )
                .multiply(
                        BigDecimal.valueOf(
                                100L
                        )
                )
                .divide(
                        BigDecimal.valueOf(
                                denominator
                        ),
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // VALIDATE DATE RANGE
    // KIỂM TRA KHOẢNG NGÀY
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
                    "The start date must not be later than the end date."
            );
        }
    }


    // =========================================================
    // SAFE LONG
    // CHUYỂN LONG AN TOÀN
    // =========================================================

    private long safeLong(
            Long value
    ) {

        return value == null
                ? 0L
                : value;
    }


    // =========================================================
    // SAFE BIG DECIMAL
    // CHUYỂN BIGDECIMAL AN TOÀN
    // =========================================================

    private BigDecimal safeBigDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}