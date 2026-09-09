package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository
        extends JpaRepository<Warehouses, Long> {

    Optional<Warehouses> findByWarehousesCode(
            String warehousesCode
    );

    boolean existsByWarehousesCode(
            String warehousesCode
    );

    List<Warehouses> findByStatus(
            WarehouseStatus status
    );

    List<Warehouses> findByTypeAndStatus(
            WarehouseType type,
            WarehouseStatus status
    );
}