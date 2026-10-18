
package com.toystorage.backend.repository.businessstaff;

import com.toystorage.backend.entity.receipts.PurchaseOrders;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessPurchaseOrderRepository
        extends JpaRepository<PurchaseOrders, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT po
            FROM PurchaseOrders po
            WHERE po.id = :id
            """)
    Optional<PurchaseOrders> findLockedById(
            @Param("id") Long id
    );
}
