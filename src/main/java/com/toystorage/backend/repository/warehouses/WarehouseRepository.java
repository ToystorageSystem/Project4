package com.toystorage.backend.repository.warehouses;

import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository
        extends JpaRepository<Warehouses, Long> {

    List<Warehouses> findByTypeAndStatusOrderByNameAsc(
            WarehouseType type,
            WarehouseStatus status
    );

    // =====================================================
    // INVENTORY REPORT
    // =====================================================

    List<Warehouses> findByStatusOrderByNameAsc(
            WarehouseStatus status
    );
}