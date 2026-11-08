package com.toystorage.backend.repository.transfers.picking;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    List<StockTransfer>
    findByFromWarehouseIdAndStatusInOrderByCreatedAtDesc(
            Long warehouseId,
            List<TransferStatus> statuses
    );

    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE st.fromWarehouse.id = :warehouseId
          AND st.status = :status
          AND (
                :keyword = ''
                OR LOWER(st.transferCode)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(st.stockTransfersCode)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<StockTransfer> findPackingConfirmations(
            @Param("warehouseId") Long warehouseId,
            @Param("status") TransferStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /*
     * Business Manager tìm kiếm và lọc phiếu điều chuyển.
     */
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

    /*
     * Phiếu điều chuyển NORMAL do người dùng hiện tại tạo.
     */
    @EntityGraph(
            attributePaths = {
                    "fromWarehouse",
                    "toWarehouse"
            }
    )
    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE st.createdBy.id = :createdById
          AND st.transferType = :transferType
          AND (
                :status IS NULL
                OR st.status = :status
          )
          AND (
                :keyword IS NULL
                OR LOWER(st.transferCode)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<StockTransfer> findMyNormalTransfers(
            @Param("createdById") Long createdById,
            @Param("transferType") TransferType transferType,
            @Param("keyword") String keyword,
            @Param("status") TransferStatus status,
            Pageable pageable
    );

    /*
     * Lấy chi tiết phiếu cùng kho, người tạo và sản phẩm.
     */
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
        SELECT DISTINCT st
        FROM StockTransfer st
        WHERE st.id = :id
    """)
    Optional<StockTransfer> findDetailedById(
            @Param("id") Long id
    );

    boolean existsByTransferCode(
            String transferCode
    );

    boolean existsByStockTransfersCode(
            String stockTransfersCode
    );
}