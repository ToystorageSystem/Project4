
package com.toystorage.backend.repository.businessstaff;

import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessPurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItems, Long> {

    List<PurchaseOrderItems>
    findByPurchaseOrder_IdOrderByIdAsc(Long purchaseOrderId);
}
