package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.inventories.InventoryTransactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementAnalyticsRepository
        extends JpaRepository<InventoryTransactions, Long> {

    // =========================================================
    // INVENTORY MOVEMENT AGGREGATE
    //
    // Object[]:
    //
    // [0] warehouseId
    // [1] productId
    // [2] totalInbound
    // [3] totalOutbound
    // [4] transactionCount
    // [5] lastMovementAt
    // =========================================================

    @Query("""
        SELECT
            t.warehouse.id,
            t.product.id,

            SUM(
                CASE
                    WHEN t.quantityChange > 0
                    THEN t.quantityChange
                    ELSE 0
                END
            ),

            SUM(
                CASE
                    WHEN t.quantityChange < 0
                    THEN -t.quantityChange
                    ELSE 0
                END
            ),

            COUNT(t.id),

            MAX(t.createdAt)

        FROM InventoryTransactions t

        WHERE
            (
                :warehouseId IS NULL
                OR t.warehouse.id = :warehouseId
            )

        AND (
                :productId IS NULL
                OR t.product.id = :productId
            )

        AND (
                :fromDate IS NULL
                OR t.createdAt >= :fromDate
            )

        AND (
                :toDate IS NULL
                OR t.createdAt <= :toDate
            )

        GROUP BY
            t.warehouse.id,
            t.product.id

        ORDER BY
            t.warehouse.id ASC,
            t.product.id ASC
    """)
    List<Object[]> findMovementAggregates(

            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // OUTBOUND MOVEMENT AGGREGATE
    //
    // Dùng cho phân tích slow-moving inventory.
    //
    // Chỉ lấy transaction làm GIẢM tồn:
    //
    // quantityChange < 0
    //
    // Không phụ thuộc cứng vào transactionType vì các nghiệp vụ
    // làm giảm tồn có thể gồm:
    //
    // - TRANSFER_OUT
    // - STORE_RETURN_OUT
    // - ADJUSTMENT_DECREASE
    // - DAMAGED
    // - EXPIRED
    // - ...
    //
    // Object[]:
    //
    // [0] warehouseId
    // [1] productId
    // [2] outboundQuantity
    // [3] outboundTransactionCount
    // [4] lastOutboundAt
    // =========================================================

    @Query("""
        SELECT
            t.warehouse.id,
            t.product.id,

            SUM(
                -t.quantityChange
            ),

            COUNT(t.id),

            MAX(t.createdAt)

        FROM InventoryTransactions t

        WHERE
            t.quantityChange < 0

        AND (
                :warehouseId IS NULL
                OR t.warehouse.id = :warehouseId
            )

        AND (
                :productId IS NULL
                OR t.product.id = :productId
            )

        AND (
                :fromDate IS NULL
                OR t.createdAt >= :fromDate
            )

        AND (
                :toDate IS NULL
                OR t.createdAt <= :toDate
            )

        GROUP BY
            t.warehouse.id,
            t.product.id

        ORDER BY
            t.warehouse.id ASC,
            t.product.id ASC
    """)
    List<Object[]> findOutboundAggregates(

            @Param("warehouseId")
            Long warehouseId,

            @Param("productId")
            Long productId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );
}