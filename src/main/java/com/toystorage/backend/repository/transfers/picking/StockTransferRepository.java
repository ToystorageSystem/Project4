package com.toystorage.backend.repository.transfers.picking;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.transfers.TransferStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    List<StockTransfer>
    findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<TransferStatus> status
    );

    // =====================================================
    // STOCK TRANSFER CREATION
    // =====================================================

    boolean existsByTransferCode(
            String transferCode
    );

    boolean existsByStockTransfersCode(
            String stockTransfersCode
    );

    // =====================================================
    // STOCK TRANSFER DETAIL
    // =====================================================

    @EntityGraph(
            attributePaths = {
                    "fromWarehouse",
                    "toWarehouse",
                    "createdBy",
                    "confirmedBy",
                    "items",
                    "items.product"
            }
    )
    @Query("""
            select distinct st
            from StockTransfer st
            where st.id = :id
            """)
    Optional<StockTransfer> findDetailedById(
            @Param("id") Long id
    );
}