package com.toystorage.backend.repository.receipts.business;

import com.toystorage.backend.entity.receipts.PurchaseOrderCancellationRequests;
import com.toystorage.backend.enums.receipts.PurchaseOrderCancellationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderCancellationRepository
        extends JpaRepository<PurchaseOrderCancellationRequests, Long> {

    boolean existsByPurchaseOrder_IdAndStatus(
            Long purchaseOrderId,
            PurchaseOrderCancellationStatus status
    );

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier", "requestedBy", "reviewedBy"})
    List<PurchaseOrderCancellationRequests> findByPurchaseOrder_IdOrderByRequestedAtDesc(Long purchaseOrderId);

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier", "requestedBy", "reviewedBy"})
    List<PurchaseOrderCancellationRequests> findByStatusOrderByRequestedAtDesc(PurchaseOrderCancellationStatus status);

    @EntityGraph(attributePaths = {"purchaseOrder", "purchaseOrder.supplier", "requestedBy", "reviewedBy"})
    List<PurchaseOrderCancellationRequests> findAllByOrderByRequestedAtDesc();

    @Query("select r.purchaseOrder.id from PurchaseOrderCancellationRequests r where r.id = :id")
    Optional<Long> findOrderIdByRequestId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PurchaseOrderCancellationRequests r where r.id = :id")
    Optional<PurchaseOrderCancellationRequests> findForReviewUpdate(@Param("id") Long id);
}
