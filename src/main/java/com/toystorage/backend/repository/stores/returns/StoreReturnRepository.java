package com.toystorage.backend.repository.stores.returns;

import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.enums.stores.StoreReturnStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreReturnRepository
        extends JpaRepository<StoreReturns, Long> {

    // Existing function
    List<StoreReturns> findByWarehouseIdOrderByCreatedAtDesc(
            Long warehouseId
    );

    // Issue #19: Find returns by status
    List<StoreReturns> findByStatusOrderByCreatedAtDesc(
            StoreReturnStatus status
    );

    // Issue #19: Lock return request during approval
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sr
        FROM StoreReturns sr
        WHERE sr.id = :id
    """)
    Optional<StoreReturns> findByIdForApproval(
            @Param("id") Long id
    );
    
    // Issue #19: Lock Store Return linked to a shipment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sr
        FROM StoreReturns sr
        WHERE sr.stockTransfer.id = :transferId
    """)
    Optional<StoreReturns> findByTransferIdForShipment(
            @Param("transferId") Long transferId
    );

}
