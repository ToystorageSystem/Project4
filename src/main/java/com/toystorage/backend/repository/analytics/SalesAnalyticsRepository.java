package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.stores.SalesOrders;
import com.toystorage.backend.enums.stores.SalesOrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalesAnalyticsRepository
        extends JpaRepository<SalesOrders, Long> {


    // =========================================================
    // TOTAL ORDERS
    // =========================================================

    @Query("""
        SELECT COUNT(so)
        FROM SalesOrders so
        WHERE (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
    """)
    Long countOrders(

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // ORDER COUNT BY STATUS
    // =========================================================

    @Query("""
        SELECT COUNT(so)
        FROM SalesOrders so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
    """)
    Long countOrdersByStatus(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // TOTAL AMOUNT BY STATUS
    // =========================================================

    @Query("""
        SELECT COALESCE(
            SUM(so.totalAmount),
            0
        )
        FROM SalesOrders so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
    """)
    BigDecimal sumOrderAmountByStatus(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // SOLD QUANTITY BY STATUS
    // =========================================================

    @Query("""
        SELECT COALESCE(
            SUM(soi.quantity),
            0
        )
        FROM SalesOrderItems soi
        JOIN soi.salesOrder so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
    """)
    Long sumSoldQuantityByStatus(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // DAILY SALES TREND
    //
    // Object[]:
    //
    // [0] period
    // [1] revenue
    // [2] completedOrders
    // =========================================================

    @Query("""
        SELECT
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m-%d'
            ),
            COALESCE(
                SUM(so.totalAmount),
                0
            ),
            COUNT(so)
        FROM SalesOrders so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
        GROUP BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m-%d'
            )
        ORDER BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m-%d'
            )
    """)
    List<Object[]> findDailyOrderTrend(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // DAILY SOLD QUANTITY
    //
    // Object[]:
    //
    // [0] period
    // [1] soldQuantity
    // =========================================================

    @Query("""
        SELECT
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m-%d'
            ),
            COALESCE(
                SUM(soi.quantity),
                0
            )
        FROM SalesOrderItems soi
        JOIN soi.salesOrder so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
        GROUP BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m-%d'
            )
        ORDER BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m-%d'
            )
    """)
    List<Object[]> findDailySoldQuantityTrend(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // MONTHLY SALES TREND
    //
    // Object[]:
    //
    // [0] period
    // [1] revenue
    // [2] completedOrders
    // =========================================================

    @Query("""
        SELECT
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m'
            ),
            COALESCE(
                SUM(so.totalAmount),
                0
            ),
            COUNT(so)
        FROM SalesOrders so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
        GROUP BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m'
            )
        ORDER BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m'
            )
    """)
    List<Object[]> findMonthlyOrderTrend(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // MONTHLY SOLD QUANTITY
    // =========================================================

    @Query("""
        SELECT
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m'
            ),
            COALESCE(
                SUM(soi.quantity),
                0
            )
        FROM SalesOrderItems soi
        JOIN soi.salesOrder so
        WHERE so.status = :status
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :fromDate IS NULL
            OR so.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR so.createdAt <= :toDate
        )
        GROUP BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m'
            )
        ORDER BY
            function(
                'DATE_FORMAT',
                so.createdAt,
                '%Y-%m'
            )
    """)
    List<Object[]> findMonthlySoldQuantityTrend(

            @Param("status")
            SalesOrderStatus status,

            @Param("storeId")
            Long storeId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );
}