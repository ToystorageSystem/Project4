package com.toystorage.backend.dto.response.receipts;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptInspectionResponse {

    private Long id;

    private String receiptInspectionsCode;

    private Long goodsReceiptId;
    private Long productId;

    private String packageCode;

    private Integer expectedQuantity;
    private Integer actualQuantity;

    private String inspectedResult;

    private String notes;

    private Long inspectedBy;

    private LocalDateTime inspectedAt;
}