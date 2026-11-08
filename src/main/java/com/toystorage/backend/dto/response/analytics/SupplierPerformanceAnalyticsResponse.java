package com.toystorage.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPerformanceAnalyticsResponse {

    // =========================================================
    // SUPPLIER
    // =========================================================

    private Long supplierId;

    private String supplierName;

    private String supplierStatus;


    // =========================================================
    // HISTORICAL SUPPLIER PERFORMANCE
    //
    // Dữ liệu lấy từ supplier_performance.
    //
    // Supplier chưa từng được đánh giá có thể không có record.
    // =========================================================

    private Boolean performanceHistoryAvailable;

    private String supplierPerformanceCode;

    private Long totalDeliveries;

    private Long onTimeDeliveries;

    private Long shortageCount;

    private Long damagedCount;

    private BigDecimal accuracyRate;

    /**
     * onTimeDeliveries / totalDeliveries * 100
     */
    private BigDecimal onTimeDeliveryRate;

    private LocalDateTime evaluatedAt;


    // =========================================================
    // PURCHASE ORDER PERFORMANCE
    // =========================================================

    private Long totalPurchaseOrders;

    private Long draftOrders;

    private Long orderedOrders;

    private Long partiallyReceivedOrders;

    private Long completedOrders;

    private Long cancelledOrders;


    // =========================================================
    // SUPPLIER RESPONSE
    // =========================================================

    private Long availableResponses;

    private Long partiallyAvailableResponses;

    private Long delayedResponses;

    private Long declinedResponses;

    private Long outOfStockResponses;

    private Long discontinuedResponses;

    private Long priceChangedResponses;

    private Long pendingResponses;


    // =========================================================
    // DELIVERY / RECEIVING
    // =========================================================

    private Long overdueOrders;

    private Long totalOrderedQuantity;

    private Long totalReceivedQuantity;

    /**
     * totalReceivedQuantity / totalOrderedQuantity * 100
     */
    private BigDecimal receivingRate;


    // =========================================================
    // PURCHASE VALUE
    // =========================================================

    /**
     * SUM(orderedQuantity * unitPrice)
     */
    private BigDecimal totalPurchaseAmount;


    // =========================================================
    // ANALYTICS
    // =========================================================

    /**
     * Ví dụ:
     *
     * OVERDUE_ORDERS
     * WAITING_SUPPLIER_RESPONSE
     * SUPPLIER_DELAYED
     * SUPPLIER_DECLINED
     * SUPPLY_ISSUE
     * NO_PERFORMANCE_HISTORY
     * NORMAL
     */
    private String supplierAnalysis;


    /**
     * Nhận xét / đề xuất cho Business Manager.
     *
     * Không tự sửa dữ liệu Supplier,
     * Purchase Order hoặc Supplier Performance.
     */
    private String recommendation;
}