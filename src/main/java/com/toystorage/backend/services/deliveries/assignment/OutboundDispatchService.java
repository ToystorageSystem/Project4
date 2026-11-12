package com.toystorage.backend.services.deliveries.assignment;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.packages.packing.PackageRepository;
import com.toystorage.backend.repository.packages.packing.PackageTransferItemRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestTransferRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;
import com.toystorage.backend.services.shipments.ShipmentManifestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound (to a store or another warehouse) after packing is finished:
 *  1. packages are checked and the shipment manifest is created (no extra manager approval),
 *  2. a delivery trip is created,
 *  3. the nearest suitable driver is chosen automatically (smart dispatch).
 * If nobody can take the trip right now it stays "waiting for driver" and
 * {@link OutboundDispatchScheduler} retries every minute.
 */
@Service
@RequiredArgsConstructor
public class OutboundDispatchService {

    private final StockTransferRepository stockTransferRepository;
    private final PackageTransferItemRepository packageTransferItemRepository;
    private final PackageRepository packageRepository;
    private final ShipmentManifestTransferRepository shipmentManifestTransferRepository;
    private final ShipmentManifestService shipmentManifestService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;

    public record DispatchResult(Long deliveryId, String shipmentCode, String manifestCode,
                                 String status, String driverName) {
    }

    @Transactional
    public DispatchResult dispatchPackedTransfer(Long transferId, Users actor) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new BadRequest("Stock transfer not found: " + transferId));
        return dispatch(transfer, actor);
    }

    @Transactional
    public DispatchResult dispatch(StockTransfer transfer, Users actor) {

        if (transfer.getStatus() != TransferStatus.PACKED) {
            throw new BadRequest("Only packed stock transfers can be dispatched");
        }
        if (transfer.getToWarehouse() == null) {
            throw new BadRequest("Stock transfer has no destination");
        }

        ShipmentManifests manifest = shipmentManifestTransferRepository
                .findByTransferId(transfer.getId())
                .map(ShipmentManifestTransfer::getManifest)
                .orElseGet(() -> createManifest(transfer, actor));

        Deliveries delivery = deliveryRepository
                .findTopByManifestIdOrderByCreatedAtDesc(manifest.getId())
                .orElseGet(() -> createDelivery(transfer, manifest));

        boolean waiting = delivery.getDeliveryStatus() == DeliveryStatus.CREATED
                || delivery.getDeliveryStatus() == DeliveryStatus.REJECTED;
        if (waiting && delivery.getHandedOverAt() == null) {
            deliveryAssignmentService.tryAutoAssign(delivery.getId());
            delivery = deliveryRepository.findById(delivery.getId()).orElse(delivery);
        }

        return new DispatchResult(
                delivery.getId(),
                delivery.getShipmentCode(),
                manifest.getManifestCode(),
                delivery.getDeliveryStatus().name(),
                delivery.getDriver() == null ? null : delivery.getDriver().getName());
    }

    /** Staff already sealed every package; mark them checked and print the manifest. */
    private ShipmentManifests createManifest(StockTransfer transfer, Users actor) {
        List<PackageTransferItem> links = packageTransferItemRepository.findByStockTransferId(transfer.getId());
        if (links.isEmpty()) {
            throw new BadRequest("Stock transfer has no packages");
        }
        LocalDateTime now = LocalDateTime.now();
        for (PackageTransferItem link : links) {
            Packages pack = link.getPackageEntity();
            if (pack.getSealNumber() == null || pack.getSealNumber().isBlank()) {
                throw new BadRequest("Package " + pack.getPackagesCode() + " is not sealed");
            }
            if (pack.getStatus() == PackageStatus.PACKED) {
                pack.setStatus(PackageStatus.CHECKED);
                pack.setCheckedBy(actor);
                pack.setUpdatedAt(now);
                packageRepository.save(pack);
            }
        }
        return shipmentManifestService.createManifest(transfer, links, actor);
    }

    private Deliveries createDelivery(StockTransfer transfer, ShipmentManifests manifest) {
        LocalDateTime now = LocalDateTime.now();
        Deliveries delivery = Deliveries.builder()
                .shipmentCode("SHP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .fromWarehouse(transfer.getFromWarehouse())
                .toWarehouse(transfer.getToWarehouse())
                .manifest(manifest)
                .deliveryStatus(DeliveryStatus.CREATED)
                .expectedPickupAt(now)
                .expectedDeliveryAt(transfer.getExpectedReceiptDate() == null
                        ? null : transfer.getExpectedReceiptDate().atTime(17, 0))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return deliveryRepository.save(delivery);
    }
}
