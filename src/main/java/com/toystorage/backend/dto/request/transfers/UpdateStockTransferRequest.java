package com.toystorage.backend.dto.request.transfers;

import com.toystorage.backend.enums.transfers.TransferReasonCode;
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
public class UpdateStockTransferRequest {

    @NotNull(message = "Destination warehouse id is required")
    @Positive(message = "Destination warehouse id must be greater than 0")
    private Long toWarehouseId;

    @NotNull(message = "Expected shipment date is required")
    private LocalDate expectedShipmentDate;

    @NotNull(message = "Expected receipt date is required")
    private LocalDate expectedReceiptDate;

    @NotNull(message = "Transfer reason is required")
    private TransferReasonCode reasonCode;

    private String reasonNote;

    private String notes;

    @Valid
    @NotEmpty(message = "Transfer items are required")
    private List<CreateStockTransferItemRequest> items;
}