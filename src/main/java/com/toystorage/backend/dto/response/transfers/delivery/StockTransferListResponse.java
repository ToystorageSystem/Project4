
package com.toystorage.backend.dto.response.transfers.delivery;

import com.toystorage.backend.enums.transfers.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferListResponse {

    private Long id;

    private String transferCode;

    private TransferStatus status;

    private String fromWarehouseName;

    private String toWarehouseName;

    private LocalDate expectedShipmentDate;

    private LocalDate expectedReceiptDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
