package com.toystorage.backend.dto.response.warehouses;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DamagedGoodsReportResponse {

    private Long reportId;

    private String reportCode;

    private String sourceType;
    private Long sourceId;

    private String description;
    private String status;

    private Long warehouseId;

    private Long reportedBy;
    private String reportedByName;

    private Long reviewedBy;
    private String reviewedByName;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;

    private List<DamagedGoodsItemResponse> items;
}
