package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.ReceiptInspections;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptInspectionRepository
        extends JpaRepository<ReceiptInspections, Long> {

    List<ReceiptInspections> findByGoodsReceiptId(Long goodsReceiptId);
    long countByGoodsReceiptId(
            Long goodsReceiptId
    );
    boolean existsByGoodsReceiptIdAndProductId(
            Long goodsReceiptId,
            Long productId
    );
}