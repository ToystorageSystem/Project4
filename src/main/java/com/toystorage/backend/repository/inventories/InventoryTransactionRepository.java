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

    /*
     * Lấy lịch sử giữ và hoàn trả tồn kho
     * của một chứng từ theo thứ tự phát sinh.
     */
    @Query("""
        SELECT t
        FROM InventoryTransactions t
        WHERE t.referenceType = :referenceType
          AND t.referenceId = :referenceId
          AND t.transactionType IN :transactionTypes
        ORDER BY t.id ASC
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