package com.toystorage.backend.dto.response.suppliers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoiceDetailResponse {

    private Long id;

    private String invoiceCode;

    private String invoiceNumber;

    private String invoiceSeries;

    /*
     * Supplier information
     */
    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private String supplierTaxCode;

    /*
     * Purchase Order information
     */
    private Long purchaseOrderId;

    private String purchaseOrderCode;

    private String purchaseOrderStatus;

    /*
     * Invoice information
     */
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    private BigDecimal subtotal;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String lookupCode;

    private String xmlFileUrl;

    private String pdfFileUrl;

    private String imageFileUrl;

    private String status;

    private String verificationNote;

    /*
     * User uploaded invoice
     */
    private Long uploadedById;

    private String uploadedByName;

    /*
     * Purchase Order reconciliation
     */
    private BigDecimal purchaseOrderAmount;

    private BigDecimal amountDifference;

    private Boolean purchaseOrderMatched;

    private String comparisonWarning;

    /*
     * Các dòng hàng lấy trực tiếp từ Purchase Order.
     *
     * supplier_invoices hiện không có invoice_items riêng.
     */
    private List<SupplierInvoicePurchaseOrderItemResponse> purchaseOrderItems;

    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;
}