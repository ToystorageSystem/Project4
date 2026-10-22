package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @Query("""
    SELECT item
    FROM GoodsReceiptItems item
    JOIN FETCH item.product
    WHERE item.goodsReceipt.id = :goodsReceiptId
    ORDER BY item.id ASC
""")
List<GoodsReceiptItems> findDetailsByGoodsReceiptId(
        @Param("goodsReceiptId") Long goodsReceiptId
);

}