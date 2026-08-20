package com.toystorage.backend.dto.response.warehouses.putaway;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PutawayPlanResponse {

    private Long id;

    private String putawayCode;

    private Long goodsReceiptId;

    private Long warehouseId;

    private String status;

    private Long assignedTo;
    private String assignedToName;

    private Long createdBy;
    private String createdByName;

    private Integer totalItems;
    private Integer completedItems;

    private Integer totalQuantity;
    private Integer putawayQuantity;

    private Double progressPercent;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private List<PutawayPlanItemResponse> items;
}