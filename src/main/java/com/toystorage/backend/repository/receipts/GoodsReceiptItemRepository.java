package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoodsReceiptItemRepository
        extends JpaRepository<GoodsReceiptItems, Long> {

    List<GoodsReceiptItems>
    findByGoodsReceiptId(Long goodsReceiptId);

    Optional<GoodsReceiptItems>
    findByGoodsReceiptIdAndProductId(
            Long goodsReceiptId,
            Long productId
    );
    boolean existsByGoodsReceiptIdAndProductId(
            Long goodsReceiptId,
            Long productId
    );

    long countByGoodsReceiptId(Long goodsReceiptId);

}