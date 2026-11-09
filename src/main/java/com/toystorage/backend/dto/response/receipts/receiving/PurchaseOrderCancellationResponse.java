package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PurchaseOrderCancellationResponse {
    private Long id;
    private Long purchaseOrderId;
    private String orderCode;
    private String supplierName;
    private String purchaseOrderStatus;
    private String status;
    private String reason;
    private Boolean supplierAgreed;
    private Long requestedById;
    private String requestedByName;
    private LocalDateTime requestedAt;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String decisionNote;
}
