package com.toystorage.backend.repository.transfers.picking;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.transfers.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer>
    findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<TransferStatus> status
    );
    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE st.fromWarehouse.id = :warehouseId
          AND st.status = :status
          AND (
                :keyword = ''
                OR LOWER(st.transferCode)
                    LIKE LOWER(
                        CONCAT(
                            '%',
                            :keyword,
                            '%'
                        )
                    )
                OR LOWER(st.stockTransfersCode)
                    LIKE LOWER(
                        CONCAT(
                            '%',
                            :keyword,
                            '%'
                        )
                    )
          )
        """)
    Page<StockTransfer>
    findPackingConfirmations(

            @Param("warehouseId")
            Long warehouseId,

            @Param("status")
            TransferStatus status,

            @Param("keyword")
            String keyword,

            Pageable pageable
    );
}