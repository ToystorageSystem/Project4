
package com.toystorage.backend.repository.transfers.picking;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.transfers.TransferStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    List<StockTransfer>
    findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<TransferStatus> status
    );

    // Phuong thuc hien tai - giu nguyen
    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE st.fromWarehouse.id = :warehouseId
          AND st.status = :status
          AND (
                :keyword = ''
                OR LOWER(st.transferCode)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
                OR LOWER(st.stockTransfersCode)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
          )
        """)
    Page<StockTransfer> findPackingConfirmations(
            @Param("warehouseId") Long warehouseId,
            @Param("status") TransferStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Issue #21: Tim kiem phieu xuat cho Business Manager
    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE (
            :keyword IS NULL
            OR LOWER(st.transferCode)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(st.stockTransfersCode)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (
            :status IS NULL
            OR st.status = :status
        )
        AND (
            :warehouseId IS NULL
            OR st.fromWarehouse.id = :warehouseId
        )
        AND (
            :fromDate IS NULL
            OR st.createdAt >= :fromDate
        )
        AND (
            :toDate IS NULL
            OR st.createdAt < :toDate
        )
        """)
    Page<StockTransfer> searchForBusinessManager(
            @Param("keyword") String keyword,
            @Param("status") TransferStatus status,
            @Param("warehouseId") Long warehouseId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );


    // Methods restored for legacy business APIs without replacing existing repository logic.

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

    // =====================================================
        // STOCK TRANSFER CREATION
        // =====================================================

        boolean existsByTransferCode(
                String transferCode
        );

    boolean existsByStockTransfersCode(
                String stockTransfersCode
        );
}
