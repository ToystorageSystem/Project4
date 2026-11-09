package com.toystorage.backend.dto.response.warehouses.location;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WarehouseLocationResponse {

    private Long id;

    /*
     * Warehouse
     */
    private Long warehouseId;

    private String warehousesCode;

    private String warehouseName;


    /*
     * Location
     */
    private String warehouseCode;

    private String warehouseLocationsCode;

    private String name;

    private String zone;

    private String shelf;

    private String locationType;

    private String status;
}