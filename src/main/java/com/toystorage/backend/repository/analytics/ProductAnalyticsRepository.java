package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.products.Products;

import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.stores.SalesOrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductAnalyticsRepository
        extends JpaRepository<Products, Long> {


    // =========================================================
    // PRODUCTS USED FOR ANALYTICS
    //
    // Lấy toàn bộ sản phẩm theo trạng thái.
    //
    // categoryId == null:
    //      lấy tất cả danh mục.
    //
    // categoryId != null:
    //      chỉ lấy sản phẩm thuộc danh mục yêu cầu.
    // =========================================================

    @Query("""
        SELECT DISTINCT p
        FROM Products p
        JOIN FETCH p.category c
        WHERE p.status = :productStatus
        AND (
            :categoryId IS NULL
            OR c.id = :categoryId
        )
        ORDER BY
            p.name ASC,
            p.id ASC
    """)
    List<Products> findProductsForAnalytics(

            @Param("productStatus")
            ProductStatus productStatus,

            @Param("categoryId")
            Long categoryId
    );


    // =========================================================
    // COMPLETED SALES BY PRODUCT
    //
    // Object[]:
    //
    // [0] productId
    // [1] soldQuantity
    // [2] revenue
    // [3] completedOrderCount
    //
    // Chỉ thống kê item thuộc đơn có trạng thái truyền vào.
    //
    // Với Dashboard Product Analytics:
    //
    // orderStatus = COMPLETED
    // =========================================================

    @Query("""
        SELECT
            p.id,
            SUM(soi.quantity),
            SUM(soi.subtotal),
            COUNT(DISTINCT so.id)
        FROM SalesOrderItems soi
        JOIN soi.product p
        JOIN soi.salesOrder so
        WHERE so.status = :orderStatus
        AND (
            :storeId IS NULL
            OR so.store.id = :storeId
        )
        AND (
            :categoryId IS NULL
            OR p.category.id = :categoryId
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
            p.id
        ORDER BY
            SUM(soi.quantity) DESC,
            SUM(soi.subtotal) DESC,
            p.id ASC
    """)
    List<Object[]> findProductSalesAggregates(

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