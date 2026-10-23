package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateGoodsReceiptRequest {

    @NotNull(message = "Supplier id is required")
    @Positive(message = "Supplier id must be greater than 0")
    private Long supplierId;

    @NotNull(message = "Warehouse id is required")
    @Positive(message = "Warehouse id must be greater than 0")
    private Long warehouseId;

    @Valid
    @NotEmpty(message = "At least one product is required")
    private List<CreateGoodsReceiptItemRequest> items;

    @Size(
            max = 2000,
            message = "Note must not exceed 2000 characters"
    )
    private String note;
}