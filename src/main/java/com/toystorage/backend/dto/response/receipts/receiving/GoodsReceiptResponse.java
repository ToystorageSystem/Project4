package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptResponse {

    private Long id;

    private String goodsReceiptsCode;
    private String receiptCode;

    private Long purchaseOrderId;
    private Long warehouseId;

    private String status;

    private Long confirmedBy;
    private Long receivedBy;

    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}