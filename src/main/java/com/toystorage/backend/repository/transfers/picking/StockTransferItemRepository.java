
package com.toystorage.backend.repository.transfers.picking;

import com.toystorage.backend.entity.transfers.StockTransferItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferItemRepository
        extends JpaRepository<StockTransferItems, Long> {

    // Cac phuong thuc cu - giu nguyen

    List<StockTransferItems> findByStockTransferId(
            Long stockTransferId
    );

    Optional<StockTransferItems> findByIdAndStockTransferId(
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
    Optional<StockTransferItems> findByTransferAndBarcode(
            @Param("transferId") Long transferId,
            @Param("barcode") String barcode
    );

    // Issue #21: Lay san pham trong chi tiet phieu xuat
    @Query("""
        SELECT item
        FROM StockTransferItems item
        JOIN FETCH item.product
        WHERE item.stockTransfer.id = :transferId
        ORDER BY item.id ASC
    """)
    List<StockTransferItems> findDetailsByStockTransferId(
            @Param("transferId") Long transferId
    );
}
