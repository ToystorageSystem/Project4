
package com.toystorage.backend.dto.response.businessmanager;

import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTransferListResponse {

    private Long id;

    private String transferCode;

    private Long fromWarehouseId;
    private String fromWarehouseName;

    private Long toWarehouseId;
    private String toWarehouseName;

    private TransferType transferType;
    private TransferStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
}
