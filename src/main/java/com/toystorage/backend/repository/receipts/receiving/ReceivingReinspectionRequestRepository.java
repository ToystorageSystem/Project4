package com.toystorage.backend.repository.receipts.receiving;

import com.toystorage.backend.entity.receipts.ReceivingReinspectionRequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceivingReinspectionRequestRepository
        extends JpaRepository<ReceivingReinspectionRequest, Long> {

    List<ReceivingReinspectionRequest>
    findAllByGoodsReceiptIdOrderByRequestedAtDesc(
            Long goodsReceiptId
    );

    Optional<ReceivingReinspectionRequest>
    findFirstByGoodsReceiptIdOrderByRequestedAtDesc(
            Long goodsReceiptId
    );
}