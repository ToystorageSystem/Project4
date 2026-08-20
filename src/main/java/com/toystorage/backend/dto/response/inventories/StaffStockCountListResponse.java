package com.toystorage.backend.dto.response.inventories;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StaffStockCountListResponse {

    private Long stockCountId;

    private String countCode;

    private String countType;

    private LocalDate scheduledDate;

    private String status;


    private Long warehouseId;

    private String warehouseName;


    private Long assignedToId;

    private String assignedToName;


    private Integer totalItems;

    private Integer countedItems;

    private Integer remainingItems;


    /*
     * true = chưa ai nhận, current Staff có thể Start.
     */
    private Boolean available;
}