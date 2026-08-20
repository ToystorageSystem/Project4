package com.toystorage.backend.services.warehouses.location;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.warehouses.WarehouseLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseLocationService {

    private final WarehouseLocationRepository warehouseLocationRepository;

    /*
     * KHU VỰC NHẬN HÀNG
     */
    public WarehouseLocations getReceivingLocation(
            Long warehouseId
    ) {

        return warehouseLocationRepository
                .findFirstByWarehouseIdAndWarehouseCodeAndStatus(
                        warehouseId,
                        "RECEIVING",
                        WarehouseStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Receiving location not found for warehouse: "
                                        + warehouseId
                        )
                );
    }

    /*
     * VỊ TRÍ PUTAWAY ĐÍCH
     */
    public WarehouseLocations getDestinationLocation(
            Long warehouseId,
            Long productId
    ) {

        return warehouseLocationRepository
                .findFirstByWarehouseIdAndLocationTypeAndStatus(
                        warehouseId,
                        WarehouseLocationType.NORMAL,
                        WarehouseStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Normal destination location not found "
                                        + "for warehouse: "
                                        + warehouseId
                        )
                );
    }
}