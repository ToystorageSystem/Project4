
package com.toystorage.backend.repository.businessmanager;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.transfers.StockTransfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusinessActivityReportRepository
        extends JpaRepository<GoodsReceipts, Long> {

    // =========================================
    // 1. BAO CAO NHAP HANG
    // =========================================

    @Query("""
        SELECT gr
        FROM GoodsReceipts gr
        JOIN FETCH gr.warehouse w
        WHERE (:warehouseId IS NULL
            OR w.id = :warehouseId)
        AND (:fromTime IS NULL
            OR gr.createdAt >= :fromTime)
        AND (:toTimeExclusive IS NULL
            OR gr.createdAt < :toTimeExclusive)
        ORDER BY gr.createdAt DESC, gr.id DESC
        """)
    List<GoodsReceipts> findImportReceipts(
            @Param("warehouseId") Long warehouseId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTimeExclusive")
            LocalDateTime toTimeExclusive
    );

    @Query("""
        SELECT item.goodsReceipt.id,
               SUM(item.acceptedQuantity)
        FROM GoodsReceiptItems item
        WHERE item.goodsReceipt.id IN :receiptIds
        GROUP BY item.goodsReceipt.id
        """)
    List<Object[]> sumAcceptedQuantities(
            @Param("receiptIds") List<Long> receiptIds
    );

    // =========================================
    // 2. BAO CAO XUAT HANG
    // =========================================

    // Tim phieu xuat theo ngay tao va kho xuat
    @Query("""
        SELECT st
        FROM StockTransfer st
        JOIN FETCH st.fromWarehouse fw
        JOIN FETCH st.toWarehouse tw
        WHERE (:warehouseId IS NULL
            OR fw.id = :warehouseId)
        AND (:fromTime IS NULL
            OR st.createdAt >= :fromTime)
        AND (:toTimeExclusive IS NULL
            OR st.createdAt < :toTimeExclusive)
        ORDER BY st.createdAt DESC, st.id DESC
        """)
    List<StockTransfer> findExportTransfers(
            @Param("warehouseId") Long warehouseId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTimeExclusive")
            LocalDateTime toTimeExclusive
    );

    // Tong so luong da xuat theo tung phieu
    @Query("""
        SELECT item.stockTransfer.id,
               SUM(item.shippedQuantity)
        FROM StockTransferItems item
        WHERE item.stockTransfer.id IN :transferIds
        GROUP BY item.stockTransfer.id
        """)
    List<Object[]> sumShippedQuantities(
            @Param("transferIds") List<Long> transferIds
    );

    // =========================================
    // 3. BAO CAO DIEU CHUYEN
    // =========================================

    // Tim phieu dieu chuyen theo ngay tao
    // va kho gui hoac kho nhan
    @Query("""
        SELECT st
        FROM StockTransfer st
        JOIN FETCH st.fromWarehouse fw
        JOIN FETCH st.toWarehouse tw
        WHERE (:warehouseId IS NULL
            OR fw.id = :warehouseId
            OR tw.id = :warehouseId)
        AND (:fromTime IS NULL
            OR st.createdAt >= :fromTime)
        AND (:toTimeExclusive IS NULL
            OR st.createdAt < :toTimeExclusive)
        ORDER BY st.createdAt DESC, st.id DESC
        """)
    List<StockTransfer> findTransferActivities(
            @Param("warehouseId") Long warehouseId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTimeExclusive")
            LocalDateTime toTimeExclusive
    );
}
