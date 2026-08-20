package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.warehouses.WarehouseLocationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DamagedGoodsLocationService {

    private final WarehouseLocationRepository
            warehouseLocationRepository;

    public WarehouseLocations getQuarantineLocation(
            Long warehouseId
    ) {

        return warehouseLocationRepository
                .findFirstByWarehouseIdAndLocationTypeAndStatus(
                        warehouseId,
                        WarehouseLocationType.QUARANTINE,
                        WarehouseStatus.ACTIVE
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Quarantine location not found"
                        )
                );
    }
}