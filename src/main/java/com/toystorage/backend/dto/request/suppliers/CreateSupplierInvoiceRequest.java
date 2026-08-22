package com.toystorage.backend.dto.request.suppliers;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateSupplierInvoiceRequest {

    @NotNull(message = "Supplier id is required")
    @Positive(message = "Supplier id must be greater than 0")
    private Long supplierId;

    @NotNull(message = "Purchase order id is required")
    @Positive(message = "Purchase order id must be greater than 0")
    private Long purchaseOrderId;

    @NotBlank(message = "Invoice number is required")
    @Size(
            max = 100,
            message = "Invoice number must not exceed 100 characters"
    )
    private String invoiceNumber;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(
            value = "0.00",
            message = "Subtotal must be greater than or equal to 0"
    )
    private BigDecimal subtotal;

    @NotNull(message = "Tax amount is required")
    @DecimalMin(
            value = "0.00",
            message = "Tax amount must be greater than or equal to 0"
    )
    private BigDecimal taxAmount;

    @NotNull(message = "Total amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Total amount must be greater than 0"
    )
    private BigDecimal totalAmount;
}