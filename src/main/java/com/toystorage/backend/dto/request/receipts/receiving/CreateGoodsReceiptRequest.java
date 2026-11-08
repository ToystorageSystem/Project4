package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateGoodsReceiptRequest {

    @NotNull(message = "Purchase order id is required")
    @Positive(message = "Purchase order id must be greater than 0")
    private Long purchaseOrderId;

    @Valid
    @NotEmpty(message = "At least one product is required")
    private List<CreateGoodsReceiptItemRequest> items;
}