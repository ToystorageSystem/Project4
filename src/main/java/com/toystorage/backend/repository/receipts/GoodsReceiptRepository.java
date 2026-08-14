package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipts, Long> {

    Optional<GoodsReceipts> findByReceiptCode(String receiptCode);

    Optional<GoodsReceipts> findByGoodsReceiptsCode(String goodsReceiptsCode);

    List<GoodsReceipts> findByWarehouseIdAndStatus(
            Long warehouseId,
            GoodsReceiptStatus status
    );
    List<GoodsReceipts>
    findByWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<GoodsReceiptStatus> statuses
    );
}