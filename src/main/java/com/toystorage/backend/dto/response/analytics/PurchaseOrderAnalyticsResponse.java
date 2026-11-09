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
public class PurchaseOrderAnalyticsResponse {

    // =========================================================
    // PURCHASE ORDER
    // =========================================================

    private Long purchaseOrderId;

    private String orderCode;

    private String status;


    // =========================================================
    // SUPPLIER
    // =========================================================

    private Long supplierId;

    private String supplierName;

    private String supplierResponseStatus;

    private LocalDateTime supplierRespondedAt;


    // =========================================================
    // WAREHOUSE
    // =========================================================

    private Long warehouseId;

    private String warehouseName;


    // =========================================================
    // DATE
    // =========================================================

    private LocalDate expectedDeliveryDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    // =========================================================
    // ORDER SUMMARY
    // =========================================================

    /**
     * Số loại sản phẩm trong PO.
     */
    private Long totalProducts;


    /**
     * Tổng số lượng đặt.
     */
    private Long totalOrderedQuantity;


    /**
     * Tổng số lượng supplier đã xác nhận.
     */
    private Long totalConfirmedQuantity;


    /**
     * Tổng số lượng đã nhận thực tế.
     */
    private Long totalReceivedQuantity;


    // =========================================================
    // VALUE
    // =========================================================

    /**
     * Tổng giá trị đặt hàng:
     *
     * orderedQuantity * unitPrice
     */
    private BigDecimal totalOrderedAmount;


    /**
     * Tổng giá trị supplier xác nhận:
     *
     * confirmedQuantity * confirmedUnitPrice
     *
     * Có thể null/0 nếu supplier chưa phản hồi.
     */
    private BigDecimal totalConfirmedAmount;


    // =========================================================
    // PROGRESS
    // =========================================================

    /**
     * Tỷ lệ nhận hàng:
     *
     * totalReceivedQuantity / totalOrderedQuantity * 100
     */
    private BigDecimal receivingProgressPercent;


    // =========================================================
    // ANALYTICS
    // =========================================================

    /**
     * Ví dụ:
     *
     * ON_TIME
     * OVERDUE
     * COMPLETED
     * CANCELLED
     * NO_EXPECTED_DATE
     */
    private String deliveryStatus;


    /**
     * Số ngày quá hạn.
     *
     * = 0 nếu chưa quá hạn.
     */
    private Long overdueDays;


    /**
     * Ví dụ:
     *
     * WAITING_SUPPLIER_RESPONSE
     * SUPPLIER_AVAILABLE
     * SUPPLIER_DELAYED
     * SUPPLIER_DECLINED
     * PRICE_CHANGED
     */
    private String supplierAnalysis;


    /**
     * Nhận xét / đề xuất cho Business Manager.
     */
    private String recommendation;
}