package com.toystorage.backend.repository.inventories;

import com.toystorage.backend.entity.inventories.InventoryTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransactions, Long> {
}