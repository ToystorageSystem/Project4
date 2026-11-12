package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.analytics.AnalyticResults;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ForecastAnalyticsRepository
        extends JpaRepository<AnalyticResults, Long> {


    // =========================================================
    // FORECAST BASE DATA
    //
    // Dữ liệu nền dùng để forecast:
    //
    // - product
    // - store
    // - tồn kho hiện tại
    // - lịch sử completed sales
    //
    // Chỉ đọc.
    // KHÔNG ghi analytics_results.
    // =========================================================

    interface ForecastBaseRow {

        // PRODUCT
        Long getProductId();

        String getProductCode();

        String getProductName();

        String getBaseUnit();


        // STORE
        Long getStoreId();

        String getStoreCode();

        String getStoreName();


        // INVENTORY
        Long getCurrentQuantity();

        Long getReservedQuantity();

        Long getAvailableQuantity();


        // SALES HISTORY
        LocalDate getHistoryStartDate();

        LocalDate getHistoryEndDate();

        Long getHistoryCalendarDays();

        Long getObservedSalesDays();

        Long getCompletedOrders();

        Long getTotalSoldQuantity();

        BigDecimal getTotalRevenue();
    }


    // =========================================================
    // STORED ANALYTICS RESULT
    //
    // Dữ liệu đã tồn tại trong analytics_results.
    //
    // Phải tách riêng khỏi forecast tính realtime để tránh
    // hiểu nhầm seed/sample data là kết quả mới.
    // =========================================================

    interface StoredAnalyticsRow {

        Long getStoredAnalyticsResultId();

        String getStoredAnalyticsCode();

        String getStoredAnalysisType();

        LocalDateTime getStoredGeneratedAt();

        String getStoredResultData();

        BigDecimal getStoredScore();

        Long getProductId();

        Long getStoreId();

        Long getWarehouseId();
    }


    // =========================================================
    // FORECAST BASE DATA QUERY
    //
    // Bắt đầu từ inventory_balances để:
    //
    // - sản phẩm có tồn kho nhưng chưa từng bán vẫn xuất hiện
    // - không phụ thuộc stored available_quantity
    //
    // available =
    // quantity - reserved_quantity
    //
    // Sales chỉ tính:
    //
    // status = COMPLETED
    //
    // Có thể lọc:
    //
    // - storeId
    // - productId
    // - fromDate
    // - toDate
    // =========================================================

    @Query(
            value = """
                    SELECT

                        /* =====================================
                           PRODUCT
                           ===================================== */

                        p.id
                            AS productId,

                        p.products_code
                            AS productCode,

                        p.name
                            AS productName,

                        p.base_unit
                            AS baseUnit,


                        /* =====================================
                           STORE
                           ===================================== */

                        w.id
                            AS storeId,

                        w.warehouses_code
                            AS storeCode,

                        w.name
                            AS storeName,


                        /* =====================================
                           CURRENT INVENTORY
                           ===================================== */

                        inv.current_quantity
                            AS currentQuantity,

                        inv.reserved_quantity
                            AS reservedQuantity,

                        inv.available_quantity
                            AS availableQuantity,


                        /* =====================================
                           SALES HISTORY
                           ===================================== */

                        sales.history_start_date
                            AS historyStartDate,

                        sales.history_end_date
                            AS historyEndDate,

                        COALESCE(
                            sales.history_calendar_days,
                            0
                        ) AS historyCalendarDays,

                        COALESCE(
                            sales.observed_sales_days,
                            0
                        ) AS observedSalesDays,

                        COALESCE(
                            sales.completed_orders,
                            0
                        ) AS completedOrders,

                        COALESCE(
                            sales.total_sold_quantity,
                            0
                        ) AS totalSoldQuantity,

                        COALESCE(
                            sales.total_revenue,
                            0
                        ) AS totalRevenue


                    /* =========================================
                       CURRENT INVENTORY
                       ========================================= */

                    FROM (

                        SELECT

                            ib.warehouse_id
                                AS store_id,

                            ib.product_id
                                AS product_id,


                            COALESCE(
                                SUM(
                                    ib.quantity
                                ),
                                0
                            ) AS current_quantity,


                            COALESCE(
                                SUM(
                                    ib.reserved_quantity
                                ),
                                0
                            ) AS reserved_quantity,


                            COALESCE(
                                SUM(
                                    COALESCE(
                                        ib.quantity,
                                        0
                                    )
                                    -
                                    COALESCE(
                                        ib.reserved_quantity,
                                        0
                                    )
                                ),
                                0
                            ) AS available_quantity


                        FROM inventory_balances ib


                        JOIN warehouses iw

                            ON iw.id =
                               ib.warehouse_id


                        WHERE

                            iw.type = 'STORE'

                            AND

                            (
                                :storeId IS NULL
                                OR ib.warehouse_id =
                                   :storeId
                            )

                            AND

                            (
                                :productId IS NULL
                                OR ib.product_id =
                                   :productId
                            )


                        GROUP BY

                            ib.warehouse_id,

                            ib.product_id

                    ) inv


                    /* =========================================
                       STORE
                       ========================================= */

                    JOIN warehouses w

                        ON w.id =
                           inv.store_id


                    /* =========================================
                       PRODUCT
                       ========================================= */

                    JOIN products p

                        ON p.id =
                           inv.product_id


                    /* =========================================
                       SALES HISTORY
                       ========================================= */

                    LEFT JOIN (

                        SELECT

                            so.store_id
                                AS store_id,

                            soi.product_id
                                AS product_id,


                            MIN(
                                DATE(
                                    so.created_at
                                )
                            ) AS history_start_date,


                            MAX(
                                DATE(
                                    so.created_at
                                )
                            ) AS history_end_date,


                            CASE

                                WHEN COUNT(
                                    DISTINCT DATE(
                                        so.created_at
                                    )
                                ) = 0

                                THEN 0

                                ELSE

                                    DATEDIFF(

                                        MAX(
                                            DATE(
                                                so.created_at
                                            )
                                        ),

                                        MIN(
                                            DATE(
                                                so.created_at
                                            )
                                        )

                                    ) + 1

                            END
                                AS history_calendar_days,


                            COUNT(
                                DISTINCT DATE(
                                    so.created_at
                                )
                            ) AS observed_sales_days,


                            COUNT(
                                DISTINCT so.id
                            ) AS completed_orders,


                            COALESCE(
                                SUM(
                                    soi.quantity
                                ),
                                0
                            ) AS total_sold_quantity,


                            COALESCE(
                                SUM(
                                    soi.quantity
                                    *
                                    soi.unit_price
                                ),
                                0
                            ) AS total_revenue


                        FROM sales_orders so


                        JOIN sales_order_items soi

                            ON soi.sales_order_id =
                               so.id


                        WHERE

                            so.status =
                                'COMPLETED'

                            AND

                            (
                                :storeId IS NULL
                                OR so.store_id =
                                   :storeId
                            )

                            AND

                            (
                                :productId IS NULL
                                OR soi.product_id =
                                   :productId
                            )

                            AND

                            (
                                :fromDate IS NULL
                                OR so.created_at >=
                                   :fromDate
                            )

                            AND

                            (
                                :toDate IS NULL
                                OR so.created_at <=
                                   :toDate
                            )


                        GROUP BY

                            so.store_id,

                            soi.product_id

                    ) sales

                        ON sales.store_id =
                           inv.store_id

                        AND

                        sales.product_id =
                            inv.product_id


                    ORDER BY

                        w.id ASC,

                        p.id ASC
                    """,
            nativeQuery = true
    )
    List<ForecastBaseRow>
    findForecastBaseData(

            @Param("storeId")
            Long storeId,

            @Param("productId")
            Long productId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );


    // =========================================================
    // LATEST STORED ANALYTICS RESULT
    //
    // Ví dụ:
    //
    // DEMAND_FORECAST
    // SALES_FORECAST
    // STOCK_RISK
    //
    // Lấy record mới nhất theo:
    //
    // generated_at DESC
    // id DESC
    // =========================================================

    @Query(
            value = """
                    SELECT

                        ar.id
                            AS storedAnalyticsResultId,

                        ar.analytics_results_code
                            AS storedAnalyticsCode,

                        ar.analysis_type
                            AS storedAnalysisType,

                        ar.generated_at
                            AS storedGeneratedAt,

                        ar.result_data
                            AS storedResultData,

                        ar.score
                            AS storedScore,

                        ar.product_id
                            AS productId,

                        ar.store_id
                            AS storeId,

                        ar.warehouse_id
                            AS warehouseId


                    FROM analytics_results ar


                    WHERE

                        ar.product_id =
                            :productId

                        AND

                        ar.store_id =
                            :storeId

                        AND

                        ar.analysis_type =
                            :analysisType


                    ORDER BY

                        ar.generated_at DESC,

                        ar.id DESC


                    LIMIT 1
                    """,
            nativeQuery = true
    )
    List<StoredAnalyticsRow>
    findLatestStoredAnalytics(

            @Param("productId")
            Long productId,

            @Param("storeId")
            Long storeId,

            @Param("analysisType")
            String analysisType
    );
}