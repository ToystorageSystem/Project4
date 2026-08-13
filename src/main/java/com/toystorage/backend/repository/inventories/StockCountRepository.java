package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.StockCounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockCountRepository
        extends JpaRepository<StockCounts, Long> {

    List<StockCounts>
    findByWarehouseIdOrderByIdDesc(
            Long warehouseId
    );
}