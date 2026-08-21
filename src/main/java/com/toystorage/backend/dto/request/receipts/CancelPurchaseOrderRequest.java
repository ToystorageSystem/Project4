package com.toystorage.backend.dto.request.receipts;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelPurchaseOrderRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}