package com.toystorage.backend.dto.response.receipts.receiving;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingIssueResponse {

    private Long productId;

    private String productName;

    private Integer expectedQuantity;

    private Integer actualQuantity;

    private String inspectionResult;

    private String notes;
}