package com.toystorage.backend.repository.inventories.lowstock;

import com.toystorage.backend.entity.inventories.LowStockReport;
import com.toystorage.backend.enums.warehouses.LowStockReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LowStockReportRepository extends JpaRepository<LowStockReport, Long> {

    @Query("""
        SELECT r FROM LowStockReport r JOIN FETCH r.warehouse JOIN FETCH r.createdBy LEFT JOIN FETCH r.handledBy
        WHERE r.warehouse.id = :warehouseId ORDER BY r.createdAt DESC
        """)
    List<LowStockReport> findForWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("""
        SELECT r FROM LowStockReport r JOIN FETCH r.warehouse JOIN FETCH r.createdBy LEFT JOIN FETCH r.handledBy
        WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC
        """)
    List<LowStockReport> findAllForBusiness(@Param("status") LowStockReportStatus status);

    @Query("""
        SELECT DISTINCT r FROM LowStockReport r JOIN FETCH r.warehouse JOIN FETCH r.createdBy
        LEFT JOIN FETCH r.handledBy LEFT JOIN FETCH r.items i LEFT JOIN FETCH i.product
        WHERE r.id = :id
        """)
    Optional<LowStockReport> findDetail(@Param("id") Long id);
}
