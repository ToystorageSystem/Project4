package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingItemResponse {

    private Long productId;

    private String productCode;
    private String productName;
    private String barcode;

    private Integer expectedQuantity;
    private Integer actualQuantity;

    private Integer acceptedQuantity;
    private Integer damagedQuantity;

    private Integer shortageQuantity;
    private Integer surplusQuantity;

    private Integer differenceQuantity;

    private String inspectionResult;

    private String packageCode;

    private String notes;

    private Boolean inspected;
}