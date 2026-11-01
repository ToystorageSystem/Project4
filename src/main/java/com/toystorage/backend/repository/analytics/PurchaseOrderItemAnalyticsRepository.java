package com.toystorage.backend.repository.analytics;

import com.toystorage.backend.entity.receipts.PurchaseOrderItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PurchaseOrderItemAnalyticsRepository
        extends JpaRepository<PurchaseOrderItems, Long> {


    // =========================================================
    // PURCHASE ORDER ITEM ANALYTICS
    //
    // Lấy toàn bộ item của nhiều Purchase Order trong một query.
    //
    // Dùng cho:
    //
    // - tổng số lượng đặt
    // - tổng số lượng xác nhận
    // - tổng số lượng đã nhận
    // - tổng giá trị đặt hàng
    // - tổng giá trị NCC xác nhận
    // =========================================================

    @Query("""
            SELECT item
            FROM PurchaseOrderItems item

            WHERE item.purchaseOrder.id IN :purchaseOrderIds

            ORDER BY
                item.purchaseOrder.id ASC,
                item.id ASC
            """)
    List<PurchaseOrderItems> findAllByPurchaseOrderIds(

            @Param("purchaseOrderIds")
            Collection<Long> purchaseOrderIds
    );
}