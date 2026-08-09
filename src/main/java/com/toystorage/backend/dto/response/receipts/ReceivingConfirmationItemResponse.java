package com.toystorage.backend.dto.response.receipts;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingConfirmationItemResponse {

    private Long productId;
    private String productName;

    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer acceptedQuantity;

    private Integer damagedQuantity;
    private Integer shortageQuantity;
    private Integer surplusQuantity;

    private String inspectionResult;
}