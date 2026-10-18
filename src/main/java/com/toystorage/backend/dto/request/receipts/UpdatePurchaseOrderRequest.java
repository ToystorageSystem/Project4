package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdatePurchaseOrderRequest {

    @NotNull(message = "Supplier id is required")
    @Positive(message = "Supplier id must be greater than 0")
    private Long supplierId;


    @NotNull(message = "Warehouse id is required")
    @Positive(message = "Warehouse id must be greater than 0")
    private Long warehouseId;


    @NotNull(message = "Expected delivery date is required")
    private LocalDate expectedDeliveryDate;


    private String note;


    @Valid
    @NotEmpty(message = "Purchase order items are required")
    private List<PurchaseOrderItemRequest> items;
}