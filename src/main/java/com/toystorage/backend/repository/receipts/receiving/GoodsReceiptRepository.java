package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipts, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from GoodsReceipts e where e.id = :id")
    Optional<GoodsReceipts> findByIdForUpdate(@Param("id") Long id);


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
}