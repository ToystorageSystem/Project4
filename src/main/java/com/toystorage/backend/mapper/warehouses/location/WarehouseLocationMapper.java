package com.toystorage.backend.mapper.warehouses.location;

import com.toystorage.backend.dto.response.warehouses.location.WarehouseLocationResponse;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import org.springframework.stereotype.Component;

@Component
public class WarehouseLocationMapper {

    public WarehouseLocationResponse toResponse(
            WarehouseLocations location
    ) {

        if (location == null) {
            return null;
        }

        return WarehouseLocationResponse
                .builder()

                .id(
                        location.getId()
                )

                .warehouseId(
                        location.getWarehouse() != null
                                ? location.getWarehouse().getId()
                                : null
                )

                .warehousesCode(
                        location.getWarehouse() != null
                                ? location.getWarehouse().getWarehousesCode()
                                : null
                )

                .warehouseName(
                        location.getWarehouse() != null
                                ? location.getWarehouse().getName()
                                : null
                )

                .warehouseCode(
                        location.getWarehouseCode()
                )

                .warehouseLocationsCode(
                        location.getWarehouseLocationsCode()
                )

                .name(
                        location.getName()
                )

                .zone(
                        location.getZone()
                )

                .shelf(
                        location.getShelf()
                )

                .locationType(
                        location.getLocationType() != null
                                ? location.getLocationType().name()
                                : null
                )

                .status(
                        location.getStatus() != null
                                ? location.getStatus().name()
                                : null
                )

                .build();
    }
}