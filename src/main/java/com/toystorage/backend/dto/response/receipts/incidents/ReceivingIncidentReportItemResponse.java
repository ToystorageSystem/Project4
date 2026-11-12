package com.toystorage.backend.dto.response.receipts.incidents;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingIncidentReportItemResponse {

    private Long id;
    private Long discrepancyReportId;
    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;
    private String discrepancyType;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer acceptedQuantity;
    private Integer damagedQuantity;
    private Integer shortageQuantity;
    private Integer surplusQuantity;
    private String reason;
}
