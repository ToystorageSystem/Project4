package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.inventory.InventoryBalances;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAnalyticsRepository
        extends JpaRepository<InventoryBalances, Long> {


    // =========================================================
    // ALL WAREHOUSES
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0)
        FROM InventoryBalances i
    """)
    Long getTotalQuantity();


    @Query("""
        SELECT COALESCE(SUM(i.reservedQuantity), 0)
        FROM InventoryBalances i
    """)
    Long getTotalReservedQuantity();


    /*
     * Không cộng trực tiếp availableQuantity đang lưu trong DB,
     * vì dữ liệu cũ có thể chưa đồng bộ.
     *
     * Luôn tính:
     *
     * available = quantity - reservedQuantity
     */
    @Query("""
        SELECT
            COALESCE(SUM(i.quantity), 0)
            -
            COALESCE(SUM(i.reservedQuantity), 0)
        FROM InventoryBalances i
    """)
    Long getTotalAvailableQuantity();


    // =========================================================
    // BY WAREHOUSE
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0)
        FROM InventoryBalances i
        WHERE i.warehouse.id = :warehouseId
    """)
    Long getTotalQuantityByWarehouse(

            @Param("warehouseId")
            Long warehouseId
    );


    @Query("""
        SELECT COALESCE(SUM(i.reservedQuantity), 0)
        FROM InventoryBalances i
        WHERE i.warehouse.id = :warehouseId
    """)
    Long getTotalReservedQuantityByWarehouse(

            @Param("warehouseId")
            Long warehouseId
    );


    @Query("""
        SELECT
            COALESCE(SUM(i.quantity), 0)
            -
            COALESCE(SUM(i.reservedQuantity), 0)
        FROM InventoryBalances i
        WHERE i.warehouse.id = :warehouseId
    """)
    Long getTotalAvailableQuantityByWarehouse(

            @Param("warehouseId")
            Long warehouseId
    );


    // =========================================================
    // INVENTORY ROWS OF WAREHOUSE
    //
    // Dùng sau này cho:
    //
    // - low stock
    // - slow-moving products
    // - transfer recommendations
    // =========================================================

    List<InventoryBalances> findByWarehouse_Id(
            Long warehouseId
    );
}