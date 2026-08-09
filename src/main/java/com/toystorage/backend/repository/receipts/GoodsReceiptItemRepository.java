package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository
        extends JpaRepository<GoodsReceiptItems, Long> {

    List<GoodsReceiptItems>
    findByGoodsReceiptId(Long goodsReceiptId);

    long countByGoodsReceiptId(Long goodsReceiptId);

}