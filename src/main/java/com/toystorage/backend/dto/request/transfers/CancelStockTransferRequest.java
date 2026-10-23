package com.toystorage.backend.dto.request.transfers;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelStockTransferRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}