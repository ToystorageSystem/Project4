package com.toystorage.backend.repository.transfers;

import com.toystorage.backend.entity.transfers.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {
}