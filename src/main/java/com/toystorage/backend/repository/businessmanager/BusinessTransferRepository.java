
package com.toystorage.backend.repository.businessmanager;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.transfers.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE (
            :keyword IS NULL
            OR LOWER(st.transferCode)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (
            :status IS NULL
            OR st.status = :status
        )
        """)
    Page<StockTransfer> searchTransfers(
            @Param("keyword") String keyword,
            @Param("status") TransferStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "fromWarehouse",
        "toWarehouse",
        "createdBy",
        "confirmedBy",
        "items",
        "items.product"
    })
    @Query("""
        SELECT st
        FROM StockTransfer st
        WHERE st.id = :id
        """)
    Optional<StockTransfer> findDetailById(
            @Param("id") Long id
    );
}
