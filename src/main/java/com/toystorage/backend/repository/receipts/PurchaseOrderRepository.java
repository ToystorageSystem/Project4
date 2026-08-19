package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.PurchaseOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrders, Long> {

    boolean existsByOrderCode(String orderCode);
}