package com.toystorage.backend.repository.receipts.store;

import com.toystorage.backend.entity.receipts.StoreReceipts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreReceiptRepository
        extends JpaRepository<StoreReceipts, Long> {

    Optional<StoreReceipts>
    findFirstByStockTransferIdOrderByCreatedAtDesc(
            Long stockTransferId
    );
}