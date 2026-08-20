package com.toystorage.backend.dto.response.warehouses.putaway;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PutawayStaffTaskResponse {

    private Long taskId;

    private String taskCode;

    private String status;

    private Long goodsReceiptId;
    private String receiptCode;

    private Long warehouseId;
    private String warehouseName;

    private Long assignedTo;
    private String assignedToName;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private List<PutawayStaffItemResponse> items;
}