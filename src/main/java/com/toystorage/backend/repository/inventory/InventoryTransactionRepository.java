package com.toystorage.backend.repository.inventory;

import com.toystorage.backend.entity.inventory.InventoryTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransactions, Long> {
}