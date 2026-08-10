package com.toystorage.backend.repository.transfers;

import com.toystorage.backend.entity.transfers.StockTransferItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferItemRepository
        extends JpaRepository<StockTransferItems, Long> {

    List<StockTransferItems>
    findByStockTransferId(Long stockTransferId);
}