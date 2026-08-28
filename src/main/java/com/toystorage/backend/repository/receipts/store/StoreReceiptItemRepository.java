package com.toystorage.backend.repository.receipts.store;

import com.toystorage.backend.entity.receipts.StoreReceiptItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreReceiptItemRepository
        extends JpaRepository<StoreReceiptItems, Long> {

    List<StoreReceiptItems>
    findByStoreReceiptId(
            Long storeReceiptId
    );

    Optional<StoreReceiptItems>
    findByStoreReceiptIdAndProductId(
            Long storeReceiptId,
            Long productId
    );
}