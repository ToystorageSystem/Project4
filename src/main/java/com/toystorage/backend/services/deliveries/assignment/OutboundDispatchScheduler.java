package com.toystorage.backend.services.deliveries.assignment;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Keeps outbound shipments moving without anyone pressing a button:
 *  - packed transfers that have no manifest/trip yet (e.g. packed before this feature) are dispatched,
 *  - trips still waiting for a driver (nobody free, or the driver rejected) are offered again.
 * Runs every minute; each item is handled in its own transaction so one failure does not block the rest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboundDispatchScheduler {

    private final EntityManager entityManager;
    private final OutboundDispatchService outboundDispatchService;
    private final DeliveryAssignmentService deliveryAssignmentService;

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void run() {
        List<StockTransfer> packedWithoutManifest = entityManager.createQuery("""
                        SELECT t FROM StockTransfer t
                        WHERE t.status = :packed
                          AND t.toWarehouse IS NOT NULL
                          AND NOT EXISTS (SELECT 1 FROM ShipmentManifestTransfer mt WHERE mt.transfer = t)
                        """, StockTransfer.class)
                .setParameter("packed", TransferStatus.PACKED)
                .setMaxResults(50)
                .getResultList();
        for (StockTransfer transfer : packedWithoutManifest) {
            try {
                outboundDispatchService.dispatchPackedTransfer(transfer.getId(), transfer.getPackingCompletedBy());
            } catch (RuntimeException e) {
                log.debug("Auto dispatch skipped for transfer {}: {}", transfer.getId(), e.getMessage());
            }
        }

        List<Long> waiting = entityManager.createQuery("""
                        SELECT d.id FROM Deliveries d
                        WHERE d.deliveryStatus IN :statuses
                          AND d.manifest IS NOT NULL
                          AND d.handedOverAt IS NULL
                        ORDER BY d.createdAt
                        """, Long.class)
                .setParameter("statuses", List.of(DeliveryStatus.CREATED, DeliveryStatus.REJECTED))
                .setMaxResults(100)
                .getResultList();
        for (Long deliveryId : waiting) {
            try {
                deliveryAssignmentService.tryAutoAssign(deliveryId);
            } catch (RuntimeException e) {
                log.debug("Auto assign skipped for delivery {}: {}", deliveryId, e.getMessage());
            }
        }
    }
}
