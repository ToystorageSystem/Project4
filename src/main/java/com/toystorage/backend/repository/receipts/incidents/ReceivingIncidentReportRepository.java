package com.toystorage.backend.repository.receipts.incidents;

import com.toystorage.backend.entity.receipts.ReceivingIncidentReports;
import com.toystorage.backend.enums.receipts.ReceivingIncidentSourceDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface ReceivingIncidentReportRepository
        extends JpaRepository<ReceivingIncidentReports, Long> {

    Page<ReceivingIncidentReports> findByWarehouseId(Long warehouseId, Pageable pageable);

    Page<ReceivingIncidentReports> findByGoodsReceiptId(Long goodsReceiptId, Pageable pageable);

    Optional<ReceivingIncidentReports> findByGoodsReceiptIdAndSourceDecision(
            Long goodsReceiptId,
            ReceivingIncidentSourceDecision sourceDecision
    );

    @EntityGraph(attributePaths = {
            "goodsReceipt",
            "warehouse",
            "warehouseStaff",
            "warehouseManager",
            "items",
            "items.product",
            "items.discrepancyReport"
    })

    @Query("""
        SELECT r
        FROM ReceivingIncidentReports r
        LEFT JOIN r.goodsReceipt gr
        LEFT JOIN r.warehouseStaff staff
        LEFT JOIN r.warehouseManager manager
        WHERE r.warehouse.id = :warehouseId
          AND (
                :keyword = ''
                OR LOWER(r.reportCode)
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(gr.receiptCode)
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(staff.name, ''))
                    LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(manager.name, ''))
                    LIKE CONCAT('%', :keyword, '%')
              )
        """)
    Page<ReceivingIncidentReports> searchByWarehouse(
            @Param("warehouseId") Long warehouseId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    Optional<ReceivingIncidentReports> findDetailById(Long id);
}
