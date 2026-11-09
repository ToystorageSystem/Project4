package com.toystorage.backend.dto.response.warehouses.damagedgoods;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StaffDamagedGoodsReportResponse {

    private Long reportId;

    private String reportCode;

    private String status;


    private String sourceType;

    private Long sourceId;


    private Long warehouseId;

    private String warehouseName;


    private Long reportedById;

    private String reportedByName;


    private String description;


    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime resolvedAt;


    private List<StaffDamagedGoodsItemResponse> items;
}