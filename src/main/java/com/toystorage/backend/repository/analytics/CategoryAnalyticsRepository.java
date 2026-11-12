package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.products.Categories;

import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.stores.SalesOrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CategoryAnalyticsRepository
        extends JpaRepository<Categories, Long> {


    // =========================================================
    // CATEGORIES USED FOR ANALYTICS
    //
    // categoryId == null:
    //      lấy toàn bộ danh mục ACTIVE.
    //
    // categoryId != null:
    //      lấy riêng danh mục được chọn.
    // =========================================================

    @Query("""
        SELECT c
        FROM Categories c
        WHERE c.status = :categoryStatus
        AND (
            :categoryId IS NULL
            OR c.id = :categoryId
        )
        ORDER BY
            c.name ASC,
            c.id ASC
    """)
    List<Categories> findCategoriesForAnalytics(

            @Param("categoryStatus")
            CommonStatus categoryStatus,

            @Param("categoryId")
            Long categoryId
    );


    // =========================================================
    // COMPLETED SALES BY CATEGORY
    //
    // Object[]:
    //
    // [0] categoryId
    // [1] soldQuantity
    // [2] revenue
    // [3] completedOrderCount
    //
    // Chỉ tính item thuộc đơn COMPLETED.
    // =========================================================

    @Query("""
        SELECT
            c.id,
            SUM(soi.quantity),
            SUM(soi.subtotal),
            COUNT(DISTINCT so.id)
        FROM SalesOrderItems soi
        JOIN soi.product p
        JOIN p.category c
        JOIN soi.salesOrder so
        WHERE so.status = :orderStatus
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :categoryId IS NULL
            OR c.id = :categoryId
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
            c.id
        ORDER BY
            SUM(soi.subtotal) DESC,
            SUM(soi.quantity) DESC,
            c.id ASC
    """)
    List<Object[]> findCategorySalesAggregates(

            @Param("orderStatus")
            SalesOrderStatus orderStatus,

            @Param("storeId")
            Long storeId,

            @Param("categoryId")
            Long categoryId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );
}