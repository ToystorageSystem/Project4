package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseLocationRepository
        extends JpaRepository<WarehouseLocations, Long> {

    List<WarehouseLocations>
    findByWarehouseIdAndStatus(
            Long warehouseId,
            WarehouseStatus status
    );

    List<WarehouseLocations>
    findByWarehouseIdAndLocationTypeAndStatus(
            Long warehouseId,
            WarehouseLocationType locationType,
            WarehouseStatus status
    );

    Optional<WarehouseLocations>
    findFirstByWarehouseIdAndLocationTypeAndStatus(
            Long warehouseId,
            WarehouseLocationType locationType,
            WarehouseStatus status
    );

    // Tìm theo mã location trong warehouse
    Optional<WarehouseLocations>
    findFirstByWarehouseIdAndWarehouseCodeAndStatus(
            Long warehouseId,
            String warehouseCode,
            WarehouseStatus status
    );
}