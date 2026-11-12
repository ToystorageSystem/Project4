package com.toystorage.backend.repository.warehouses.business;

import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository
        extends JpaRepository<Warehouses, Long>,
        JpaSpecificationExecutor<Warehouses> {

    List<Warehouses> findByTypeAndStatusOrderByNameAsc(
            WarehouseType type,
            WarehouseStatus status
    );

    List<Warehouses> findByStatusOrderByNameAsc(
            WarehouseStatus status
    );

    boolean existsByWarehousesCodeIgnoreCase(
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