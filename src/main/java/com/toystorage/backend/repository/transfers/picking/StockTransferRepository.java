package com.toystorage.backend.repository.transfers.picking;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.transfers.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer>
    findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<TransferStatus> status
    );
}