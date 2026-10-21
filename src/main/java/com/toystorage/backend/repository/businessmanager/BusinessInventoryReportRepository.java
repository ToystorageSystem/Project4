
package com.toystorage.backend.repository.businessmanager;

import com.toystorage.backend.entity.inventories.InventoryBalances;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessInventoryReportRepository
        extends JpaRepository<InventoryBalances, Long> {

    // =========================================
    // BAO CAO TON KHO HIEN TAI
    // =========================================

    @Query("""
        SELECT b
        FROM InventoryBalances b
        JOIN FETCH b.warehouse w
        JOIN FETCH b.location l
        JOIN FETCH b.product p
        WHERE (:warehouseId IS NULL
            OR w.id = :warehouseId)
        ORDER BY w.name ASC,
                 p.name ASC,
                 l.warehouseLocationsCode ASC,
                 b.id ASC
        """)
    List<InventoryBalances> findInventoryReport(
            @Param("warehouseId") Long warehouseId
    );
}
