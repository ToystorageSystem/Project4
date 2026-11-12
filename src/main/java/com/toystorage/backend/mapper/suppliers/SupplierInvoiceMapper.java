package com.toystorage.backend.mapper.suppliers;

import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceDetailResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoicePurchaseOrderItemResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoicePurchaseOrderOptionResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceSummaryResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceSupplierOptionResponse;
import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.suppliers.SupplierInvoices;
import com.toystorage.backend.entity.suppliers.Suppliers;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class SupplierInvoiceMapper {

    // =====================================================
    // SUPPLIER OPTION
    // =====================================================

    public SupplierInvoiceSupplierOptionResponse toSupplierOption(
            Suppliers supplier
    ) {

        if (supplier == null) {
            return null;
        }

        return SupplierInvoiceSupplierOptionResponse.builder()

                .id(
                        supplier.getId()
                )

                .supplierCode(
                        supplier.getSuppliersCode()
                )

                .supplierName(
                        supplier.getName()
                )

                .build();
    }


    // =====================================================
    // PURCHASE ORDER OPTION
    // =====================================================

    public SupplierInvoicePurchaseOrderOptionResponse
    toPurchaseOrderOption(
            PurchaseOrders purchaseOrder,
            List<PurchaseOrderItems> items
    ) {

        if (purchaseOrder == null) {
            return null;
        }

        return SupplierInvoicePurchaseOrderOptionResponse.builder()

                .id(
                        purchaseOrder.getId()
                )

                .orderCode(
                        purchaseOrder.getOrderCode()
                )

                .status(
                        purchaseOrder.getStatus() != null
                                ? purchaseOrder.getStatus().name()
                                : null
                )

                .expectedDeliveryDate(
                        purchaseOrder.getExpectedDeliveryDate()
                )

                .totalAmount(
                        calculatePurchaseOrderAmount(items)
                )

                .build();
    }


    // =====================================================
    // PURCHASE ORDER ITEM
    // =====================================================

    public SupplierInvoicePurchaseOrderItemResponse
    toPurchaseOrderItemResponse(
            PurchaseOrderItems item
    ) {

        if (item == null) {
            return null;
        }

        return SupplierInvoicePurchaseOrderItemResponse.builder()

                .id(
                        item.getId()
                )

                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                )

                .productCode(
                        item.getProduct() != null
                                ? item.getProduct().getProductsCode()
                                : null
                )

                .productName(
                        item.getProduct() != null
                                ? item.getProduct().getName()
                                : null
                )

                .orderedQuantity(
                        item.getOrderedQuantity()
                )

                .receivedQuantity(
                        item.getReceivedQuantity()
                )

                .unitPrice(
                        item.getUnitPrice()
                )

                .lineTotal(
                        calculateLineTotal(item)
                )

                .build();
    }


    // =====================================================
    // SUMMARY
    // =====================================================

    public SupplierInvoiceSummaryResponse toSummaryResponse(
            SupplierInvoices invoice,
            List<PurchaseOrderItems> purchaseOrderItems
    ) {

        if (invoice == null) {
            return null;
        }

        BigDecimal purchaseOrderAmount =
                calculatePurchaseOrderAmount(
                        purchaseOrderItems
                );

        BigDecimal amountDifference =
                calculateAmountDifference(
                        invoice.getSubtotal(),
                        purchaseOrderAmount
                );

        boolean matched =
                isPurchaseOrderMatched(
                        invoice.getSubtotal(),
                        purchaseOrderAmount
                );

        return SupplierInvoiceSummaryResponse.builder()

                .id(
                        invoice.getId()
                )

                .invoiceCode(
                        invoice.getInvoiceCode()
                )

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .supplierId(
                        invoice.getSupplier() != null
                                ? invoice.getSupplier().getId()
                                : null
                )

                .supplierCode(
                        invoice.getSupplier() != null
                                ? invoice
                                .getSupplier()
                                .getSuppliersCode()
                                : null
                )

                .supplierName(
                        invoice.getSupplier() != null
                                ? invoice.getSupplier().getName()
                                : null
                )

                .purchaseOrderId(
                        invoice.getPurchaseOrder() != null
                                ? invoice.getPurchaseOrder().getId()
                                : null
                )

                .purchaseOrderCode(
                        invoice.getPurchaseOrder() != null
                                ? invoice
                                .getPurchaseOrder()
                                .getOrderCode()
                                : null
                )

                .invoiceDate(
                        invoice.getInvoiceDate()
                )

                .dueDate(
                        invoice.getDueDate()
                )

                .subtotal(
                        invoice.getSubtotal()
                )

                .taxAmount(
                        invoice.getTaxAmount()
                )

                .totalAmount(
                        invoice.getTotalAmount()
                )

                .status(
                        invoice.getStatus() != null
                                ? invoice.getStatus().name()
                                : null
                )

                .purchaseOrderAmount(
                        purchaseOrderAmount
                )

                .amountDifference(
                        amountDifference
                )

                .purchaseOrderMatched(
                        matched
                )

                .comparisonWarning(
                        buildComparisonWarning(
                                invoice.getSubtotal(),
                                purchaseOrderAmount
                        )
                )

                .hasAttachment(
                        hasAttachment(invoice)
                )

                .uploadedAt(
                        invoice.getUploadedAt()
                )

                .updatedAt(
                        invoice.getUpdatedAt()
                )

                .build();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    public SupplierInvoiceDetailResponse toDetailResponse(
            SupplierInvoices invoice,
            List<PurchaseOrderItems> purchaseOrderItems
    ) {

        if (invoice == null) {
            return null;
        }

        List<PurchaseOrderItems> safeItems =
                purchaseOrderItems != null
                        ? purchaseOrderItems
                        : Collections.emptyList();

        BigDecimal purchaseOrderAmount =
                calculatePurchaseOrderAmount(
                        safeItems
                );

        BigDecimal amountDifference =
                calculateAmountDifference(
                        invoice.getSubtotal(),
                        purchaseOrderAmount
                );

        boolean matched =
                isPurchaseOrderMatched(
                        invoice.getSubtotal(),
                        purchaseOrderAmount
                );

        List<SupplierInvoicePurchaseOrderItemResponse>
                itemResponses =
                safeItems.stream()
                        .map(this::toPurchaseOrderItemResponse)
                        .toList();

        return SupplierInvoiceDetailResponse.builder()

                .id(
                        invoice.getId()
                )

                .invoiceCode(
                        invoice.getInvoiceCode()
                )

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .invoiceSeries(
                        invoice.getInvoiceSeries()
                )

                // =========================================
                // SUPPLIER
                // =========================================

                .supplierId(
                        invoice.getSupplier() != null
                                ? invoice.getSupplier().getId()
                                : null
                )

                .supplierCode(
                        invoice.getSupplier() != null
                                ? invoice
                                .getSupplier()
                                .getSuppliersCode()
                                : null
                )

                .supplierName(
                        invoice.getSupplier() != null
                                ? invoice.getSupplier().getName()
                                : null
                )

                .supplierTaxCode(
                        invoice.getSupplierTaxCode()
                )

                // =========================================
                // PURCHASE ORDER
                // =========================================

                .purchaseOrderId(
                        invoice.getPurchaseOrder() != null
                                ? invoice.getPurchaseOrder().getId()
                                : null
                )

                .purchaseOrderCode(
                        invoice.getPurchaseOrder() != null
                                ? invoice
                                .getPurchaseOrder()
                                .getOrderCode()
                                : null
                )

                .purchaseOrderStatus(
                        invoice.getPurchaseOrder() != null
                                && invoice
                                .getPurchaseOrder()
                                .getStatus() != null
                                ? invoice
                                .getPurchaseOrder()
                                .getStatus()
                                .name()
                                : null
                )

                // =========================================
                // INVOICE
                // =========================================

                .invoiceDate(
                        invoice.getInvoiceDate()
                )

                .dueDate(
                        invoice.getDueDate()
                )

                .subtotal(
                        invoice.getSubtotal()
                )

                .taxAmount(
                        invoice.getTaxAmount()
                )

                .totalAmount(
                        invoice.getTotalAmount()
                )

                .lookupCode(
                        invoice.getLookupCode()
                )

                .xmlFileUrl(
                        invoice.getXmlFileUrl()
                )

                .pdfFileUrl(
                        invoice.getPdfFileUrl()
                )

                .imageFileUrl(
                        invoice.getImageFileUrl()
                )

                .status(
                        invoice.getStatus() != null
                                ? invoice.getStatus().name()
                                : null
                )

                .verificationNote(
                        invoice.getVerificationNote()
                )

                // =========================================
                // UPLOADED BY
                // =========================================

                .uploadedById(
                        invoice.getUploadedBy() != null
                                ? invoice.getUploadedBy().getId()
                                : null
                )

                .uploadedByName(
                        invoice.getUploadedBy() != null
                                ? invoice.getUploadedBy().getName()
                                : null
                )

                // =========================================
                // RECONCILIATION
                // =========================================

                .purchaseOrderAmount(
                        purchaseOrderAmount
                )

                .amountDifference(
                        amountDifference
                )

                .purchaseOrderMatched(
                        matched
                )

                .comparisonWarning(
                        buildComparisonWarning(
                                invoice.getSubtotal(),
                                purchaseOrderAmount
                        )
                )

                .purchaseOrderItems(
                        itemResponses
                )

                .uploadedAt(
                        invoice.getUploadedAt()
                )

                .updatedAt(
                        invoice.getUpdatedAt()
                )

                .build();
    }


    // =====================================================
    // CALCULATE LINE TOTAL
    // =====================================================

    public BigDecimal calculateLineTotal(
            PurchaseOrderItems item
    ) {

        if (item == null
                || item.getOrderedQuantity() == null
                || item.getUnitPrice() == null) {

            return BigDecimal.ZERO;
        }

        return item.getUnitPrice()
                .multiply(
                        BigDecimal.valueOf(
                                item.getOrderedQuantity()
                        )
                );
    }


    // =====================================================
    // CALCULATE PURCHASE ORDER TOTAL
    // =====================================================

    public BigDecimal calculatePurchaseOrderAmount(
            List<PurchaseOrderItems> items
    ) {

        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()

                .map(
                        this::calculateLineTotal
                )

                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }


    // =====================================================
    // DIFFERENCE
    // =====================================================

    public BigDecimal calculateAmountDifference(
            BigDecimal invoiceSubtotal,
            BigDecimal purchaseOrderAmount
    ) {

        if (invoiceSubtotal == null
                || purchaseOrderAmount == null) {

            return null;
        }

        return invoiceSubtotal.subtract(
                purchaseOrderAmount
        );
    }


    // =====================================================
    // MATCH
    // =====================================================

    public boolean isPurchaseOrderMatched(
            BigDecimal invoiceSubtotal,
            BigDecimal purchaseOrderAmount
    ) {

        if (invoiceSubtotal == null
                || purchaseOrderAmount == null) {

            return false;
        }

        return invoiceSubtotal.compareTo(
                purchaseOrderAmount
        ) == 0;
    }


    // =====================================================
    // WARNING
    // =====================================================

    public String buildComparisonWarning(
            BigDecimal invoiceSubtotal,
            BigDecimal purchaseOrderAmount
    ) {

        if (invoiceSubtotal == null) {
            return "Invoice subtotal is unavailable";
        }

        if (purchaseOrderAmount == null) {
            return "Purchase order amount is unavailable";
        }

        BigDecimal difference =
                invoiceSubtotal.subtract(
                        purchaseOrderAmount
                );

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return "Invoice subtotal differs from purchase order amount by "
                + difference.abs().stripTrailingZeros().toPlainString();
    }


    // =====================================================
    // ATTACHMENT
    // =====================================================

    private boolean hasAttachment(
            SupplierInvoices invoice
    ) {

        if (invoice == null) {
            return false;
        }

        return hasText(invoice.getXmlFileUrl())
                || hasText(invoice.getPdfFileUrl())
                || hasText(invoice.getImageFileUrl());
    }


    private boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }
}