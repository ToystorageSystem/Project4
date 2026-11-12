package com.toystorage.backend.dto.response.receipts.incidents;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingIncidentReportResponse {

    private Long id;
    private String reportCode;
    private Long goodsReceiptId;
    private String receiptCode;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long warehouseStaffId;
    private String warehouseStaffCode;
    private String warehouseStaffName;
    private Long warehouseManagerId;
    private String warehouseManagerCode;
    private String warehouseManagerName;
    private String sourceDecision;
    private String penaltyAction;
    private String managerNote;
    private Integer totalIssueProducts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReceivingIncidentReportItemResponse> items;
}
