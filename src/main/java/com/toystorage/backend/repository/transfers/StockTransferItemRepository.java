package com.toystorage.backend.repository.transfers;

import com.toystorage.backend.entity.transfers.StockTransferItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferItemRepository
        extends JpaRepository<StockTransferItems, Long> {

    List<StockTransferItems>
    findByStockTransferId(
            Long stockTransferId
    );

    Optional<StockTransferItems>
    findByIdAndStockTransferId(
            Long itemId,
            Long transferId
    );

    @Query("""
        select i
        from StockTransferItems i
        join i.product p
        where i.stockTransfer.id = :transferId
          and p.barcode = :barcode
    """)
    Optional<StockTransferItems>
    findByTransferAndBarcode(
            @Param("transferId") Long transferId,
            @Param("barcode") String barcode
    );
}