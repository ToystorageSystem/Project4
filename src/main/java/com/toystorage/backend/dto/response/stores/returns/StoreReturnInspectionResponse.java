package com.toystorage.backend.dto.response.stores.returns;

import com.toystorage.backend.dto.response.inventories.discrepancy.DiscrepancyReportResponse;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StoreReturnInspectionResponse {

    private Long returnId;

    private String returnCode;

    private String returnType;

    private String status;

    private Long storeId;
    private String storeName;

    private Long warehouseId;
    private String warehouseName;

    private Integer totalRequestedQuantity;
    private Integer totalReceivedQuantity;
    private Integer totalApprovedQuantity;
    private Integer totalRejectedQuantity;

    private LocalDateTime receivedAt;

    private List<StoreReturnInspectionItemResponse> items;

    /*
     * PHẢI dùng đúng DTO:
     *
     * com.toystorage.backend.dto.response
     * .inventories.discrepancy.DiscrepancyReportResponse
     */
    private List<DiscrepancyReportResponse> discrepancies;
}