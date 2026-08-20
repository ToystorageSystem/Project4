package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingListResponse {

    private Long receiptId;

    private String receiptCode;

    private String status;

    private Long purchaseOrderId;
    private String purchaseOrderCode;

    private Long supplierId;
    private String supplierName;

    private Integer totalItems;

    private Integer totalExpectedQuantity;
}