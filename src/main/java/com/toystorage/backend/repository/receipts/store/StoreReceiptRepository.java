package com.toystorage.backend.repository.receipts.store;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.receipts.StoreReceipts;

public interface StoreReceiptRepository extends JpaRepository<StoreReceipts, Long> {

    /** receipts of the transfers in a manifest (1 receipt per transfer) */
    @EntityGraph(attributePaths = {"stockTransfer"})
    List<StoreReceipts> findByStockTransfer_IdIn(Collection<Long> transferIds);
}