package com.toystorage.backend.dto.response.inventories.stockcount;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StaffStockCountDetailResponse {

    private Long stockCountId;

    private String countCode;

    private String countType;

    private LocalDate scheduledDate;

    private String status;


    private Long warehouseId;

    private String warehouseName;


    private Long assignedToId;

    private String assignedToName;


    private LocalDateTime startedAt;

    private LocalDateTime completedAt;


    private Integer totalItems;

    private Integer countedItems;

    private Integer remainingItems;


    private Boolean recount;

    private List<StaffStockCountItemResponse> items;
}