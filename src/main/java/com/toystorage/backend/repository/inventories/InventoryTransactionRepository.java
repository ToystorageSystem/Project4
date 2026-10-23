package com.toystorage.backend.repository.inventories;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransactions, Long> {


    // Methods restored for legacy business APIs without replacing existing repository logic.

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