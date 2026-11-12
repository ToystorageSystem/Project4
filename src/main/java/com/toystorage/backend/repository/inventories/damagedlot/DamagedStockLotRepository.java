package com.toystorage.backend.repository.inventories.damagedlot;

import com.toystorage.backend.entity.inventories.DamagedStockLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DamagedStockLotRepository extends JpaRepository<DamagedStockLot, Long> {

    @Query("""
        SELECT l FROM DamagedStockLot l JOIN FETCH l.product JOIN FETCH l.location LEFT JOIN FETCH l.recordedBy
        WHERE l.warehouse.id = :warehouseId ORDER BY l.createdAt DESC
        """)
    List<DamagedStockLot> findForWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("""
        SELECT l FROM DamagedStockLot l JOIN FETCH l.product JOIN FETCH l.location
        WHERE l.sourceType = :sourceType AND l.sourceId = :sourceId
        """)
    List<DamagedStockLot> findForSource(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);
}
