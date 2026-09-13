package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.entity.receipts.ReceiptInspections;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptInspectionRepository
        extends JpaRepository<ReceiptInspections, Long> {

    List<ReceiptInspections> findByGoodsReceiptId(Long goodsReceiptId);
    long countByGoodsReceiptId(
            Long goodsReceiptId
    );

    Optional<ReceiptInspections>
    findByGoodsReceiptIdAndProductId(
            Long goodsReceiptId,
            Long productId
    );
    boolean existsByGoodsReceiptIdAndProductId(
            Long goodsReceiptId,
            Long productId
    );
    long countByTaskClaimId(
            Long taskClaimId
    );


    List<ReceiptInspections>
    findByTaskClaimId(
            Long taskClaimId
    );


    Optional<ReceiptInspections>
    findByTaskClaimIdAndProductId(
            Long taskClaimId,
            Long productId
    );
    Optional<ReceiptInspections>
    findFirstByGoodsReceiptIdAndProductIdOrderByInspectedAtDesc(
            Long goodsReceiptId,
            Long productId
    );
}