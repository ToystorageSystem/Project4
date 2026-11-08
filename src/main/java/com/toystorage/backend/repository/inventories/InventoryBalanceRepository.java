package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository
        extends JpaRepository<InventoryBalances, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM InventoryBalances b
        WHERE b.warehouse.id = :warehouseId
          AND b.location.id = :locationId
          AND b.product.id = :productId
    """)
    Optional<InventoryBalances> findBalanceForUpdate(
            @Param("warehouseId") Long warehouseId,
            @Param("locationId") Long locationId,
            @Param("productId") Long productId
    );

    Optional<InventoryBalances>
    findByWarehouseIdAndLocationIdAndProductId(
            Long warehouseId,
            Long locationId,
            Long productId
    );

    List<InventoryBalances> findByWarehouseId(
            Long warehouseId
    );

    Optional<InventoryBalances>
    findFirstByWarehouseIdAndProductId(
            Long warehouseId,
            Long productId
    );

    @Query("""
        SELECT b
        FROM InventoryBalances b
        JOIN b.location l
        WHERE b.warehouse.id = :warehouseId
          AND b.product.id = :productId
          AND l.warehouseCode = :locationCode
    """)
    Optional<InventoryBalances> findPickingBalance(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("locationCode") String locationCode
    );

    /*
     * Khóa bản ghi tồn kho khi Business Manager
     * duyệt yêu cầu trả hàng của Store.
     */
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