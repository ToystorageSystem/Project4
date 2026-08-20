package com.toystorage.backend.repository.inventories.stockcount;

import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.enums.inventories.StockCountStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StaffStockCountRepository
        extends JpaRepository<StockCounts, Long> {

    List<StockCounts>
    findByWarehouseIdAndStatusInOrderByScheduledDateAsc(
            Long warehouseId,
            Collection<StockCountStatus> statuses
    );
}