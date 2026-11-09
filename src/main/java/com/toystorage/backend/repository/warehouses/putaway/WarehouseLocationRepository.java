package com.toystorage.backend.repository.warehouses.putaway;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository
        extends JpaRepository<WarehouseLocations, Long> {


    /*
     * =====================================================
     * LIST ALL LOCATIONS OF WAREHOUSE
     * =====================================================
     */

    List<WarehouseLocations>
    findByWarehouseIdOrderByZoneAscShelfAscWarehouseCodeAsc(
            Long warehouseId
    );


    /*
     * =====================================================
     * FIND BY STATUS
     * =====================================================
     */

    List<WarehouseLocations>
    findByWarehouseIdAndStatus(
            Long warehouseId,
            WarehouseStatus status
    );


    /*
     * =====================================================
     * FIND BY LOCATION TYPE
     * =====================================================
     */

    List<WarehouseLocations>
    findByWarehouseIdAndLocationTypeAndStatus(
            Long warehouseId,
            WarehouseLocationType locationType,
            WarehouseStatus status
    );


    /*
     * =====================================================
     * FIND FIRST BY TYPE
     * =====================================================
     */

    Optional<WarehouseLocations>
    findFirstByWarehouseIdAndLocationTypeAndStatus(
            Long warehouseId,
            WarehouseLocationType locationType,
            WarehouseStatus status
    );


    /*
     * =====================================================
     * FIND ACTIVE LOCATION BY CODE
     * =====================================================
     */

    Optional<WarehouseLocations>
    findFirstByWarehouseIdAndWarehouseCodeAndStatus(
            Long warehouseId,
            String warehouseCode,
            WarehouseStatus status
    );


    /*
     * =====================================================
     * FIND LOCATION BY CODE
     * =====================================================
     */

    Optional<WarehouseLocations>
    findByWarehouseIdAndWarehouseCodeIgnoreCase(
            Long warehouseId,
            String warehouseCode
    );


    /*
     * =====================================================
     * CHECK DUPLICATE
     * =====================================================
     */

    boolean existsByWarehouseIdAndWarehouseCodeIgnoreCase(
            Long warehouseId,
            String warehouseCode
    );


    boolean existsByWarehouseIdAndWarehouseCodeIgnoreCaseAndIdNot(
            Long warehouseId,
            String warehouseCode,
            Long id
    );
}