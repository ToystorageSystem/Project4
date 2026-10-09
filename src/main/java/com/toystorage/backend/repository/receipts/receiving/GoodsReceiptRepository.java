package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;



@Repository
public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipts, Long> {

    Optional<GoodsReceipts> findByReceiptCode(
            String receiptCode
    );

    Optional<GoodsReceipts> findByGoodsReceiptsCode(
            String goodsReceiptsCode
    );

    Page<GoodsReceipts> findByWarehouseIdAndStatus(
            Long warehouseId,
            GoodsReceiptStatus status,
            Pageable pageable
    );

    List<GoodsReceipts>
    findByWarehouseIdAndStatusOrderByCreatedAtDesc(
            Long warehouseId,
            GoodsReceiptStatus status
    );

    List<GoodsReceipts>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<GoodsReceiptStatus> statuses
);
    @Query("""
        SELECT gr
        FROM GoodsReceipts gr
        WHERE (
            :keyword IS NULL
            OR LOWER(gr.receiptCode)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(gr.goodsReceiptsCode)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (
            :status IS NULL
            OR gr.status = :status
        )
        AND (
            :warehouseId IS NULL
            OR gr.warehouse.id = :warehouseId
        )
        AND (
            :fromDate IS NULL
            OR gr.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR gr.createdAt < :toDate
        )
        """)
    Page<GoodsReceipts> searchForBusinessManager(
            @Param("keyword") String keyword,
            @Param("status") GoodsReceiptStatus status,
            @Param("warehouseId") Long warehouseId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

}