package com.toystorage.backend.dto.request.warehouses.location;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWarehouseLocationStatusRequest {

    @NotNull(
            message = "Status is required"
    )
    private WarehouseStatus status;
}