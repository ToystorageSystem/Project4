package com.toystorage.backend.dto.response.inventories;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReceivingShortageReportResponse {

    private Long reportId;

    private String reportCode;

    private String status;

    private Long receiptId;
    private String receiptCode;

    private Long warehouseId;
    private String warehouseName;

    private Long reportedBy;
    private String reportedByName;

    private String description;

    private LocalDateTime createdAt;

    private List<ReceivingShortageItemResponse> items;
}