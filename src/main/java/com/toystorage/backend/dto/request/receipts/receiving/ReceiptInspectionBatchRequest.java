package com.toystorage.backend.dto.request.receipts.receiving;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceiptInspectionBatchRequest {

    @NotEmpty(message = "Inspection items are required")
    @Valid
    private List<ReceiptInspectionItemRequest> items;
}