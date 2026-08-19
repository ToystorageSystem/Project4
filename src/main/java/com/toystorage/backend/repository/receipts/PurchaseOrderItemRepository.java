package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItems, Long> {

    List<PurchaseOrderItems> findByPurchaseOrderId(
            Long purchaseOrderId
    );
}