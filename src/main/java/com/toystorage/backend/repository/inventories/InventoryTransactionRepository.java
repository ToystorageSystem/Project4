package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransactions, Long> {

    // =====================================================
    // STOCK TRANSFER - RESERVATION HISTORY
    // =====================================================

    @Query("""
            select t
            from InventoryTransactions t
            where t.referenceType = :referenceType
              and t.referenceId = :referenceId
              and t.transactionType in :transactionTypes
            order by t.id asc
            """)
    List<InventoryTransactions> findReservationHistory(
            @Param("referenceType")
            InventoryReferenceType referenceType,

            @Param("referenceId")
            Long referenceId,

            @Param("transactionTypes")
            List<InventoryTransactionType> transactionTypes
    );
}