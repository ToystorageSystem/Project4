package com.toystorage.backend.repository.receipts.store;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.receipts.StoreReceiptItems;

public interface StoreReceiptItemRepository extends JpaRepository<StoreReceiptItems, Long> {

    @EntityGraph(attributePaths = {"product", "storeReceipt"})
    List<StoreReceiptItems> findByStoreReceipt_IdIn(Collection<Long> receiptIds);
}