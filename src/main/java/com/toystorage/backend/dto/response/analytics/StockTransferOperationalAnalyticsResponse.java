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
public class StockTransferOperationalAnalyticsResponse {

    // =========================================================
    // TRANSFER
    // =========================================================

    private Long transferId;

    private String transferCode;

    private String status;

    private String transferType;

    private String reasonCode;


    // =========================================================
    // SOURCE WAREHOUSE
    // =========================================================

    private Long fromWarehouseId;

    private String fromWarehouseCode;

    private String fromWarehouseName;

    private String fromWarehouseType;


    // =========================================================
    // DESTINATION WAREHOUSE
    // =========================================================

    private Long toWarehouseId;

    private String toWarehouseCode;

    private String toWarehouseName;

    private String toWarehouseType;


    // =========================================================
    // TIMELINE
    // =========================================================

    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime packingCompletedAt;

    private LocalDateTime shippedAt;

    private LocalDateTime receivedAt;


    // =========================================================
    // EXPECTED DATES
    // =========================================================

    private LocalDate expectedShipmentDate;

    private LocalDate expectedReceiptDate;


    // =========================================================
    // PRODUCTS
    // =========================================================

    private Long totalProducts;


    // =========================================================
    // QUANTITY FLOW
    //
    // Requested
    //    ↓
    // Approved
    //    ↓
    // Picked
    //    ↓
    // Packed
    //    ↓
    // Shipped
    //    ↓
    // Received
    // =========================================================

    private Long totalRequestedQuantity;

    private Long totalApprovedQuantity;

    private Long totalPickedQuantity;

    private Long totalPackedQuantity;

    private Long totalShippedQuantity;

    private Long totalReceivedQuantity;


    // =========================================================
    // DISCREPANCY
    // =========================================================

    private Long totalShortageQuantity;

    private Long totalSurplusQuantity;


    // =========================================================
    // PROGRESS
    // =========================================================

    /**
     * shipped / approved * 100
     */
    private BigDecimal shippingProgressPercent;

    /**
     * received / shipped * 100
     *
     * Chỉ mang ý nghĩa tiến độ nhận hàng.
     * Shortage / surplus vẫn được báo riêng.
     */
    private BigDecimal receivingProgressPercent;


    // =========================================================
    // SCHEDULE ANALYTICS
    // =========================================================

    /**
     * true:
     *
     * - đã ship sau expectedShipmentDate
     *
     * hoặc
     *
     * - chưa ship và expectedShipmentDate đã qua.
     *
     * false:
     * chưa phát hiện trễ.
     *
     * null:
     * không có expectedShipmentDate.
     */
    private Boolean shipmentOverdue;


    /**
     * Số ngày trễ shipment.
     *
     * null nếu không có expectedShipmentDate.
     */
    private Long shipmentDelayDays;


    /**
     * true:
     *
     * - đã nhận sau expectedReceiptDate
     *
     * hoặc
     *
     * - chưa nhận và expectedReceiptDate đã qua.
     *
     * false:
     * chưa phát hiện trễ.
     *
     * null:
     * không có expectedReceiptDate.
     */
    private Boolean receiptOverdue;


    /**
     * Số ngày trễ receipt.
     *
     * null nếu không có expectedReceiptDate.
     */
    private Long receiptDelayDays;


    // =========================================================
    // OPERATIONAL ANALYSIS
    // =========================================================

    /**
     * Ví dụ:
     *
     * SHORTAGE
     * SURPLUS
     * OVERDUE_RECEIPT
     * OVERDUE_SHIPMENT
     * PARTIAL_RECEIVING
     * WAITING_SHIPMENT
     * COMPLETED_NORMAL
     * IN_PROGRESS
     */
    private String operationalAnalysis;


    /**
     * Recommendation cho Business Manager.
     *
     * Chỉ đưa ra nhận xét.
     * Không tự thay đổi Stock Transfer.
     */
    private String recommendation;
}