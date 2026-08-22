package com.toystorage.backend.dto.response.suppliers;

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
public class SupplierInvoiceSummaryResponse {

    private Long id;

    private String invoiceCode;

    private String invoiceNumber;

    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private Long purchaseOrderId;

    private String purchaseOrderCode;

    private LocalDate invoiceDate;

    private LocalDate dueDate;

    private BigDecimal subtotal;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String status;

    /*
     * Tổng giá trị của Purchase Order:
     *
     * SUM(orderedQuantity * unitPrice)
     */
    private BigDecimal purchaseOrderAmount;

    /*
     * invoice subtotal - purchase order amount
     */
    private BigDecimal amountDifference;

    /*
     * true nếu subtotal của hóa đơn khớp tổng tiền PO.
     */
    private Boolean purchaseOrderMatched;

    /*
     * Cảnh báo khi hóa đơn và PO bị lệch giá trị.
     * Null nếu khớp.
     */
    private String comparisonWarning;

    private Boolean hasAttachment;

    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;
}