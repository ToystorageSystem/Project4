package com.toystorage.backend.repository.receipts;

import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrders, Long> {

    boolean existsBySupplier_IdAndStatusNotIn(
            Long supplierId,
            Collection<PurchaseOrderStatus> completedStatuses
    );
}