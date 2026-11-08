package com.toystorage.backend.repository.stores;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toystorage.backend.entity.stores.StoreStockTransactions;

public interface StoreStockTransactionRepository extends JpaRepository<StoreStockTransactions, Long> {
}