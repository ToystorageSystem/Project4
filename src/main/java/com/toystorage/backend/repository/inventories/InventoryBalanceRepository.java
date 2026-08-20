package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface InventoryBalanceRepository
        extends JpaRepository<InventoryBalances, Long> {

    Optional<InventoryBalances>
    findByWarehouseIdAndLocationIdAndProductId(
            Long warehouseId,
            Long locationId,
            Long productId
    );
    List<InventoryBalances>
    findByWarehouseId(
            Long warehouseId
    );
}