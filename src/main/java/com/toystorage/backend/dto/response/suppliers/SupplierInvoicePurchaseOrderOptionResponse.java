package com.toystorage.backend.dto.response.suppliers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoicePurchaseOrderOptionResponse {

    private Long id;

    private String orderCode;

    private String status;

    private LocalDate expectedDeliveryDate;

    private BigDecimal totalAmount;
}