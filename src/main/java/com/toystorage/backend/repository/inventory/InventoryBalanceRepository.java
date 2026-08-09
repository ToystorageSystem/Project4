package com.toystorage.backend.repository.inventory;

import com.toystorage.backend.entity.inventory.InventoryBalances;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryBalanceRepository
        extends JpaRepository<InventoryBalances, Long> {

    Optional<InventoryBalances>
    findByWarehouseIdAndLocationIdAndProductId(
            Long warehouseId,
            Long locationId,
            Long productId
    );
}