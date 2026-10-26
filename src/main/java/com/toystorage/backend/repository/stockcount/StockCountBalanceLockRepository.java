package com.toystorage.backend.repository.stockcount;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface StockCountBalanceLockRepository extends JpaRepository<InventoryBalances, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBalances b where b.warehouse.id = :warehouseId and b.location.id = :locationId and b.product.id = :productId")
    Optional<InventoryBalances> lockBalance(@Param("warehouseId") Long warehouseId, @Param("locationId") Long locationId, @Param("productId") Long productId);
}
