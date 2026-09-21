package com.toystorage.backend.dto.response.warehouses;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDeactivationImpactResponse {

    private Long warehouseId;

    private String warehousesCode;

    private String warehouseName;

    private WarehouseStatus status;

    private boolean canDeactivate;

    private long totalBlockingRecords;

    private Map<String, Long> blockingByModule;
}