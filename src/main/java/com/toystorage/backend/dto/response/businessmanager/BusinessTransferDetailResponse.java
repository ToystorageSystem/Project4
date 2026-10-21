
package com.toystorage.backend.dto.response.businessmanager;

import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import java.time.LocalDateTime;
import java.util.List;
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
public class BusinessTransferDetailResponse {

    private Long id;
    private String transferCode;
    private String stockTransfersCode;

    private Long fromWarehouseId;
    private String fromWarehouseName;

    private Long toWarehouseId;
    private String toWarehouseName;

    private TransferType transferType;
    private TransferStatus status;

    private String reasonCode;
    private String reasonNote;
    private String notes;
    private String rejectionReason;

    private Long createdById;
    private String createdByName;

    private Long confirmedById;
    private String confirmedByName;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime packingCompletedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime updatedAt;

    private List<BusinessTransferItemResponse> items;
}
