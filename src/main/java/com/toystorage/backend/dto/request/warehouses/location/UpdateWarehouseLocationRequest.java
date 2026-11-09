package com.toystorage.backend.dto.request.warehouses.location;

import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWarehouseLocationRequest {

    @NotBlank(
            message = "Warehouse location code is required"
    )
    @Size(
            max = 50,
            message = "Warehouse location code must not exceed 50 characters"
    )
    private String warehouseCode;


    @NotBlank(
            message = "Location name is required"
    )
    @Size(
            max = 150,
            message = "Location name must not exceed 150 characters"
    )
    private String name;


    @Size(
            max = 50,
            message = "Zone must not exceed 50 characters"
    )
    private String zone;


    @Size(
            max = 50,
            message = "Shelf must not exceed 50 characters"
    )
    private String shelf;


    @NotNull(
            message = "Location type is required"
    )
    private WarehouseLocationType locationType;


    @NotNull(
            message = "Status is required"
    )
    private WarehouseStatus status;
}