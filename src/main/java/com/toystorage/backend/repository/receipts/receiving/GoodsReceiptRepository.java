
package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipts, Long> {

    boolean existsByPurchaseOrder_Id(Long purchaseOrderId);

    // ===== CAC PHUONG THUC HIEN CO =====

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

    // Một PO được nhận nhiều lần, nhưng không được có 2 phiếu đang xử lý đồng thời.
    boolean existsByPurchaseOrder_IdAndStatusIn(
            Long purchaseOrderId,
            List<GoodsReceiptStatus> statuses
    );

    // ===== ISSUE #21: BUSINESS MANAGER =====
    // Tim kiem phieu nhap theo ma, trang thai, kho va ngay tao.

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


    // Danh sach phieu nhap lien ket voi PO, cho Business Staff theo doi.
    // Phieu nhap chua co created_by rieng; KHONG loc gia theo nguoi tao PO.
    @EntityGraph(attributePaths = {
            "purchaseOrder", "purchaseOrder.supplier",
            "purchaseOrder.createdBy", "warehouse"
    })
    @Query("""
        SELECT gr FROM GoodsReceipts gr
        WHERE (:keyword IS NULL
            OR LOWER(gr.receiptCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(gr.goodsReceiptsCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(gr.purchaseOrder.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:purchaseOrderId IS NULL OR gr.purchaseOrder.id = :purchaseOrderId)
          AND (:warehouseId IS NULL OR gr.warehouse.id = :warehouseId)
          AND (:status IS NULL OR gr.status = :status)
          AND (:createdFrom IS NULL OR gr.createdAt >= :createdFrom)
          AND (:createdToExclusive IS NULL OR gr.createdAt < :createdToExclusive)
        """)
    Page<GoodsReceipts> searchForBusinessStaff(
            @Param("keyword") String keyword,
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("warehouseId") Long warehouseId,
            @Param("status") GoodsReceiptStatus status,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdToExclusive") LocalDateTime createdToExclusive,
            Pageable pageable
    );

    // Methods restored for legacy business APIs without replacing existing repository logic.

    List<GoodsReceipts> findByWarehouseIdOrderByCreatedAtDesc(
                Long warehouseId
        );

    boolean existsByReceiptCode(
                String receiptCode
        );

    boolean existsByGoodsReceiptsCode(
                String goodsReceiptsCode
        );
}
