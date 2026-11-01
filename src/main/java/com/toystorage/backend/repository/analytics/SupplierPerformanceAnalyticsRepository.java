package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.suppliers.Suppliers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupplierPerformanceAnalyticsRepository
        extends JpaRepository<Suppliers, Long> {


    // =========================================================
    // SUPPLIER PERFORMANCE ANALYTICS ROW
    //
    // Projection nhận dữ liệu trực tiếp từ query analytics.
    //
    // Bao gồm:
    //
    // - thông tin supplier
    // - lịch sử supplier_performance mới nhất
    // - thống kê Purchase Order
    // - phản hồi supplier
    // - số lượng đặt / nhận
    // - tổng giá trị mua
    // =========================================================

    interface SupplierPerformanceAnalyticsRow {

        Long getSupplierId();

        String getSupplierName();

        String getSupplierStatus();


        // =====================================================
        // HISTORICAL PERFORMANCE
        // =====================================================

        String getSupplierPerformanceCode();

        Long getTotalDeliveries();

        Long getOnTimeDeliveries();

        Long getShortageCount();

        Long getDamagedCount();

        BigDecimal getAccuracyRate();

        LocalDateTime getEvaluatedAt();


        // =====================================================
        // PURCHASE ORDERS
        // =====================================================

        Long getTotalPurchaseOrders();

        Long getDraftOrders();

        Long getOrderedOrders();

        Long getPartiallyReceivedOrders();

        Long getCompletedOrders();

        Long getCancelledOrders();


        // =====================================================
        // SUPPLIER RESPONSES
        // =====================================================

        Long getAvailableResponses();

        Long getPartiallyAvailableResponses();

        Long getDelayedResponses();

        Long getDeclinedResponses();

        Long getOutOfStockResponses();

        Long getDiscontinuedResponses();

        Long getPriceChangedResponses();

        Long getPendingResponses();


        // =====================================================
        // DELIVERY
        // =====================================================

        Long getOverdueOrders();

        Long getTotalOrderedQuantity();

        Long getTotalReceivedQuantity();


        // =====================================================
        // VALUE
        // =====================================================

        BigDecimal getTotalPurchaseAmount();
    }


    // =========================================================
    // SUPPLIER PERFORMANCE ANALYTICS
    //
    // Chỉ đọc dữ liệu.
    //
    // KHÔNG:
    //
    // - update supplier
    // - update supplier_performance
    // - update Purchase Order
    //
    // supplier_performance:
    // lấy record được đánh giá gần nhất của supplier.
    //
    // Purchase Order:
    // tổng hợp trước theo từng PO để tránh nhân bản amount do
    // có nhiều purchase_order_items.
    // =========================================================

    @Query(
            value = """
                    SELECT

                        s.id AS supplierId,

                        s.name AS supplierName,

                        s.status AS supplierStatus,


                        /* =====================================
                           HISTORICAL PERFORMANCE
                           ===================================== */

                        sp.supplier_performance_code
                            AS supplierPerformanceCode,

                        sp.total_deliveries
                            AS totalDeliveries,

                        sp.on_time_deliveries
                            AS onTimeDeliveries,

                        sp.shortage_count
                            AS shortageCount,

                        sp.damaged_count
                            AS damagedCount,

                        sp.accuracy_rate
                            AS accuracyRate,

                        sp.evaluated_at
                            AS evaluatedAt,


                        /* =====================================
                           PURCHASE ORDER COUNT
                           ===================================== */

                        COUNT(
                            pa.purchase_order_id
                        ) AS totalPurchaseOrders,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.status = 'DRAFT'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS draftOrders,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.status = 'ORDERED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS orderedOrders,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.status = 'PARTIALLY_RECEIVED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS partiallyReceivedOrders,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.status = 'COMPLETED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS completedOrders,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.status = 'CANCELLED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS cancelledOrders,


                        /* =====================================
                           SUPPLIER RESPONSE
                           ===================================== */

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'AVAILABLE'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS availableResponses,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'PARTIALLY_AVAILABLE'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS partiallyAvailableResponses,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'DELAYED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS delayedResponses,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'DECLINED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS declinedResponses,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'OUT_OF_STOCK'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS outOfStockResponses,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'DISCONTINUED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS discontinuedResponses,


                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.supplier_response_status
                                         = 'PRICE_CHANGED'
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS priceChangedResponses,


                        /*
                         * QUAN TRỌNG:
                         *
                         * Với LEFT JOIN, supplier không có PO phù hợp
                         * với khoảng ngày vẫn tạo một dòng có pa.* = NULL.
                         *
                         * Vì vậy phải kiểm tra purchase_order_id tồn tại
                         * trước khi coi supplier_response_status NULL
                         * là PENDING_RESPONSE.
                         */

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN pa.purchase_order_id IS NOT NULL
                                         AND (
                                             pa.supplier_response_status
                                                 = 'PENDING_RESPONSE'

                                             OR

                                             pa.supplier_response_status
                                                 IS NULL
                                         )
                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS pendingResponses,


                        /* =====================================
                           OVERDUE
                           ===================================== */

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN
                                        pa.purchase_order_id IS NOT NULL

                                        AND pa.expected_delivery_date
                                            < CURRENT_DATE

                                        AND pa.status NOT IN (
                                            'COMPLETED',
                                            'CANCELLED'
                                        )

                                    THEN 1
                                    ELSE 0
                                END
                            ),
                            0
                        ) AS overdueOrders,


                        /* =====================================
                           QUANTITY
                           ===================================== */

                        COALESCE(
                            SUM(
                                pa.ordered_quantity
                            ),
                            0
                        ) AS totalOrderedQuantity,


                        COALESCE(
                            SUM(
                                pa.received_quantity
                            ),
                            0
                        ) AS totalReceivedQuantity,


                        /* =====================================
                           PURCHASE VALUE
                           ===================================== */

                        COALESCE(
                            SUM(
                                pa.ordered_amount
                            ),
                            0
                        ) AS totalPurchaseAmount


                    FROM suppliers s


                    /* =========================================
                       LATEST SUPPLIER PERFORMANCE
                       ========================================= */

                    LEFT JOIN supplier_performance sp

                        ON sp.id = (

                            SELECT sp2.id

                            FROM supplier_performance sp2

                            WHERE sp2.supplier_id = s.id

                            ORDER BY
                                sp2.evaluated_at DESC,
                                sp2.id DESC

                            LIMIT 1
                        )


                    /* =========================================
                       PURCHASE ORDER AGGREGATION
                       ========================================= */

                    LEFT JOIN (

                        SELECT

                            po.id
                                AS purchase_order_id,

                            po.supplier_id,

                            po.status,

                            po.supplier_response_status,

                            po.expected_delivery_date,


                            COALESCE(
                                SUM(
                                    poi.ordered_quantity
                                ),
                                0
                            ) AS ordered_quantity,


                            COALESCE(
                                SUM(
                                    poi.received_quantity
                                ),
                                0
                            ) AS received_quantity,


                            COALESCE(
                                SUM(
                                    poi.ordered_quantity
                                    * poi.unit_price
                                ),
                                0
                            ) AS ordered_amount


                        FROM purchase_orders po


                        LEFT JOIN purchase_order_items poi

                            ON poi.purchase_order_id = po.id


                        WHERE

                            (
                                :fromDate IS NULL
                                OR po.created_at >= :fromDate
                            )

                            AND

                            (
                                :toDate IS NULL
                                OR po.created_at <= :toDate
                            )


                        GROUP BY

                            po.id,

                            po.supplier_id,

                            po.status,

                            po.supplier_response_status,

                            po.expected_delivery_date

                    ) pa

                        ON pa.supplier_id = s.id


                    /* =========================================
                       SUPPLIER FILTER
                       ========================================= */

                    WHERE

                        (
                            :supplierId IS NULL
                            OR s.id = :supplierId
                        )


                    /* =========================================
                       GROUP
                       ========================================= */

                    GROUP BY

                        s.id,

                        s.name,

                        s.status,

                        sp.id,

                        sp.supplier_performance_code,

                        sp.total_deliveries,

                        sp.on_time_deliveries,

                        sp.shortage_count,

                        sp.damaged_count,

                        sp.accuracy_rate,

                        sp.evaluated_at


                    ORDER BY
                        s.id
                    """,
            nativeQuery = true
    )
    List<SupplierPerformanceAnalyticsRow>
    findSupplierPerformanceAnalytics(

            @Param("supplierId")
            Long supplierId,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );
}