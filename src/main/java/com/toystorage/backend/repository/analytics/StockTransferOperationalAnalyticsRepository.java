package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.transfers.StockTransfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransferOperationalAnalyticsRepository
        extends JpaRepository<StockTransfer, Long> {


    // =========================================================
    // STOCK TRANSFER OPERATIONAL ANALYTICS ROW
    // =========================================================

    interface StockTransferOperationalAnalyticsRow {

        // =====================================================
        // TRANSFER
        // =====================================================

        Long getTransferId();

        String getTransferCode();

        String getStatus();

        String getTransferType();

        String getReasonCode();


        // =====================================================
        // SOURCE WAREHOUSE
        // =====================================================

        Long getFromWarehouseId();

        String getFromWarehouseCode();

        String getFromWarehouseName();

        String getFromWarehouseType();


        // =====================================================
        // DESTINATION WAREHOUSE
        // =====================================================

        Long getToWarehouseId();

        String getToWarehouseCode();

        String getToWarehouseName();

        String getToWarehouseType();


        // =====================================================
        // TIMELINE
        // =====================================================

        LocalDateTime getCreatedAt();

        LocalDateTime getConfirmedAt();

        LocalDateTime getPackingCompletedAt();

        LocalDateTime getShippedAt();

        LocalDateTime getReceivedAt();


        // =====================================================
        // EXPECTED DATES
        // =====================================================

        LocalDate getExpectedShipmentDate();

        LocalDate getExpectedReceiptDate();


        // =====================================================
        // PRODUCTS
        // =====================================================

        Long getTotalProducts();


        // =====================================================
        // QUANTITY FLOW
        // =====================================================

        Long getTotalRequestedQuantity();

        Long getTotalApprovedQuantity();

        Long getTotalPickedQuantity();

        Long getTotalPackedQuantity();

        Long getTotalShippedQuantity();

        Long getTotalReceivedQuantity();


        // =====================================================
        // DISCREPANCY
        // =====================================================

        Long getTotalShortageQuantity();

        Long getTotalSurplusQuantity();
    }


    // =========================================================
    // STOCK TRANSFER OPERATIONAL ANALYTICS
    //
    // Dữ liệu:
    //
    // stock_transfers
    //      +
    // stock_transfer_items
    //      +
    // source warehouse
    //      +
    // destination warehouse
    //
    // Chỉ đọc dữ liệu.
    //
    // KHÔNG:
    //
    // - thay đổi Stock Transfer
    // - thay đổi status
    // - thay đổi inventory
    // =========================================================

    @Query(
            value = """
                    SELECT

                        /* =====================================
                           TRANSFER
                           ===================================== */

                        st.id
                            AS transferId,

                        st.transfer_code
                            AS transferCode,

                        st.status
                            AS status,

                        st.transfer_type
                            AS transferType,

                        st.reason_code
                            AS reasonCode,


                        /* =====================================
                           SOURCE WAREHOUSE
                           ===================================== */

                        fw.id
                            AS fromWarehouseId,

                        fw.warehouses_code
                            AS fromWarehouseCode,

                        fw.name
                            AS fromWarehouseName,

                        fw.type
                            AS fromWarehouseType,


                        /* =====================================
                           DESTINATION WAREHOUSE
                           ===================================== */

                        tw.id
                            AS toWarehouseId,

                        tw.warehouses_code
                            AS toWarehouseCode,

                        tw.name
                            AS toWarehouseName,

                        tw.type
                            AS toWarehouseType,


                        /* =====================================
                           TIMELINE
                           ===================================== */

                        st.created_at
                            AS createdAt,

                        st.confirmed_at
                            AS confirmedAt,

                        st.packing_completed_at
                            AS packingCompletedAt,

                        st.shipped_at
                            AS shippedAt,

                        st.received_at
                            AS receivedAt,


                        /* =====================================
                           EXPECTED DATES
                           ===================================== */

                        st.expected_shipment_date
                            AS expectedShipmentDate,

                        st.expected_receipt_date
                            AS expectedReceiptDate,


                        /* =====================================
                           PRODUCTS
                           ===================================== */

                        COUNT(
                            sti.id
                        ) AS totalProducts,


                        /* =====================================
                           QUANTITY FLOW
                           ===================================== */

                        COALESCE(
                            SUM(
                                sti.requested_quantity
                            ),
                            0
                        ) AS totalRequestedQuantity,


                        COALESCE(
                            SUM(
                                sti.approved_quantity
                            ),
                            0
                        ) AS totalApprovedQuantity,


                        COALESCE(
                            SUM(
                                sti.picked_quantity
                            ),
                            0
                        ) AS totalPickedQuantity,


                        COALESCE(
                            SUM(
                                sti.packed_quantity
                            ),
                            0
                        ) AS totalPackedQuantity,


                        COALESCE(
                            SUM(
                                sti.shipped_quantity
                            ),
                            0
                        ) AS totalShippedQuantity,


                        COALESCE(
                            SUM(
                                sti.received_quantity
                            ),
                            0
                        ) AS totalReceivedQuantity,


                        /* =====================================
                           DISCREPANCY
                           ===================================== */

                        COALESCE(
                            SUM(
                                sti.shortage_quantity
                            ),
                            0
                        ) AS totalShortageQuantity,


                        COALESCE(
                            SUM(
                                sti.surplus_quantity
                            ),
                            0
                        ) AS totalSurplusQuantity


                    FROM stock_transfers st


                    /* =========================================
                       SOURCE WAREHOUSE
                       ========================================= */

                    JOIN warehouses fw

                        ON fw.id =
                           st.from_warehouse_id


                    /* =========================================
                       DESTINATION WAREHOUSE
                       ========================================= */

                    JOIN warehouses tw

                        ON tw.id =
                           st.to_warehouse_id


                    /* =========================================
                       ITEMS
                       ========================================= */

                    LEFT JOIN stock_transfer_items sti

                        ON sti.stock_transfer_id =
                           st.id


                    /* =========================================
                       FILTER
                       ========================================= */

                    WHERE

                        (
                            :fromWarehouseId IS NULL
                            OR st.from_warehouse_id =
                               :fromWarehouseId
                        )

                        AND

                        (
                            :toWarehouseId IS NULL
                            OR st.to_warehouse_id =
                               :toWarehouseId
                        )

                        AND

                        (
                            :status IS NULL
                            OR st.status =
                               :status
                        )

                        AND

                        (
                            :transferType IS NULL
                            OR st.transfer_type =
                               :transferType
                        )

                        AND

                        (
                            :fromDate IS NULL
                            OR st.created_at >=
                               :fromDate
                        )

                        AND

                        (
                            :toDate IS NULL
                            OR st.created_at <=
                               :toDate
                        )


                    /* =========================================
                       GROUP
                       ========================================= */

                    GROUP BY

                        st.id,

                        st.transfer_code,

                        st.status,

                        st.transfer_type,

                        st.reason_code,

                        fw.id,

                        fw.warehouses_code,

                        fw.name,

                        fw.type,

                        tw.id,

                        tw.warehouses_code,

                        tw.name,

                        tw.type,

                        st.created_at,

                        st.confirmed_at,

                        st.packing_completed_at,

                        st.shipped_at,

                        st.received_at,

                        st.expected_shipment_date,

                        st.expected_receipt_date


                    ORDER BY

                        st.created_at DESC,

                        st.id DESC
                    """,
            nativeQuery = true
    )
    List<StockTransferOperationalAnalyticsRow>
    findOperationalAnalytics(

            @Param("fromWarehouseId")
            Long fromWarehouseId,

            @Param("toWarehouseId")
            Long toWarehouseId,

            @Param("status")
            String status,

            @Param("transferType")
            String transferType,

            @Param("fromDate")
            LocalDateTime fromDate,

            @Param("toDate")
            LocalDateTime toDate
    );
}