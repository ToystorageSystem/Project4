package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<InventoryBalances>
    findFirstByWarehouseIdAndProductId(
            Long warehouseId,
            Long productId
    );

    @Query("""
    select b
    from InventoryBalances b
    join b.location l
    where b.warehouse.id = :warehouseId
      and b.product.id = :productId
      and l.warehouseCode = :locationCode
""")
    Optional<InventoryBalances>
    findPickingBalance(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId,

            @Param("locationCode")
            String locationCode
    );

    // Issue #19: Lock inventory during Store Return approval
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM InventoryBalances b
        WHERE b.warehouse.id = :warehouseId
          AND b.location.id = :locationId
          AND b.product.id = :productId
    """)
    Optional<InventoryBalances> findForStoreReturnApproval(
            @Param("warehouseId") Long warehouseId,
            @Param("locationId") Long locationId,
            @Param("productId") Long productId
    );
}