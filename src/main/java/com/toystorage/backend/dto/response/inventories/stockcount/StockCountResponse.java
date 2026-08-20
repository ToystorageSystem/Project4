package com.toystorage.backend.dto.response.inventories.stockcount;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StockCountResponse {

    private Long stockCountId;

    private String countCode;

    private String status;

    private Long warehouseId;
    private String warehouseName;

    private LocalDateTime startedAt;

    private Long confirmedBy;
    private String confirmedByName;

    private LocalDateTime confirmedAt;

    private List<StockCountItemResponse> items;
}