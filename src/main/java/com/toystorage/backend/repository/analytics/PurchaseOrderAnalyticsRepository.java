package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseOrderAnalyticsRepository
        extends JpaRepository<PurchaseOrders, Long> {


    // =========================================================
    // PURCHASE ORDER ANALYTICS
    //
    // Lấy danh sách Purchase Order phục vụ dashboard analytics.
    //
    // Có thể lọc:
    //
    // - supplier
    // - warehouse
    // - status
    // - khoảng thời gian tạo PO
    //
    // Fetch sẵn:
    //
    // - supplier
    // - warehouse
    //
    // để tránh phát sinh query lặp khi build response.
    // =========================================================

    @EntityGraph(
            attributePaths = {
                    "supplier",
                    "warehouse"
            }
    )
    @Query("""
            SELECT po
            FROM PurchaseOrders po

            WHERE
                (
                    :supplierId IS NULL
                    OR po.supplier.id = :supplierId
                )

            AND (
                    :warehouseId IS NULL
                    OR po.warehouse.id = :warehouseId
                )

            AND (
                    :status IS NULL
                    OR po.status = :status
                )

            AND (
                    :fromDate IS NULL
                    OR po.createdAt >= :fromDate
                )

            AND (
                    :toDate IS NULL
                    OR po.createdAt <= :toDate
                )

            ORDER BY po.createdAt DESC
            """)
    List<PurchaseOrders> findForAnalytics(

            @Param("supplierId")
            Long supplierId,

            @Param("warehouseId")
            Long warehouseId,

            @Param("status")
            PurchaseOrderStatus status,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );
}