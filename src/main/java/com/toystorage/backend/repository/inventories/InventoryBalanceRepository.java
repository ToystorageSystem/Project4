package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.enums.products.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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


    // =====================================================
    // STOCK TRANSFER - TOTAL AVAILABLE QUANTITY
    // =====================================================

    @Query("""
            select coalesce(sum(b.availableQuantity), 0)
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.product.id = :productId
            """)
    Long sumAvailableQuantity(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId
    );


    // =====================================================
    // STOCK TRANSFER - AVAILABLE PRODUCTS AT SOURCE
    // =====================================================

    @Query("""
            select b.product, coalesce(sum(b.availableQuantity), 0)
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.product.status = :productStatus
            group by b.product
            having sum(b.availableQuantity) > 0
            order by b.product.name asc
            """)
    List<Object[]> findAvailableProductsByWarehouseId(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productStatus")
            ProductStatus productStatus
    );


    // =====================================================
    // STOCK TRANSFER - LOCK INVENTORY BEFORE SUBMIT
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.product.id = :productId
            order by b.id asc
            """)
    List<InventoryBalances> findByWarehouseIdAndProductIdForUpdate(
            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId
    );


    // =====================================================
    // STOCK TRANSFER - LOCK EXACT RESERVED BALANCE
    // =====================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from InventoryBalances b
            where b.warehouse.id = :warehouseId
              and b.location.id = :locationId
              and b.product.id = :productId
            """)
    Optional<InventoryBalances> findExactBalanceForUpdate(
            @Param("warehouseId")
            Long warehouseId,

            @Param("locationId")
            Long locationId,

            @Param("productId")
            Long productId
    );
}