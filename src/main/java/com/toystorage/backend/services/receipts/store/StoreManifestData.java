package com.toystorage.backend.services.receipts.store;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.receipts.PackageReceivingChecks;
import com.toystorage.backend.entity.receipts.StoreReceipts;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.enums.receipts.InspectionMethod;
import com.toystorage.backend.enums.receipts.PackageReceivingState;
import com.toystorage.backend.enums.receipts.StoreReceiptStatus;
import com.toystorage.backend.enums.receipts.StoreReceivingStatus;

/**
 * Everything the store needs to know about ONE manifest, loaded once by StoreManifestLoader.
 * All "what state is it in?" rules live here so every API computes them the same way.
 */
public record StoreManifestData(
        Deliveries delivery,
        ShipmentManifests manifest,
        List<Packages> packages,
        List<StockTransfer> transfers,
        Map<Long, StockTransfer> transferByPackageId,
        Map<Long, List<PackageItems>> itemsByPackageId,
        List<StoreReceipts> receipts,                       // 1 per transfer, empty = not started
        Map<Long, PackageReceivingChecks> checkByPackageId) { // F1 result per package

    private static final Set<DeliveryStatus> ARRIVED_STATUSES =
            Set.of(DeliveryStatus.ARRIVED, DeliveryStatus.DELIVERED);

    /** the store may only receive after the truck has arrived */
    public boolean deliveryArrived() {
        return ARRIVED_STATUSES.contains(delivery.getDeliveryStatus());
    }

    public StoreReceivingStatus receivingStatus() {
        if (receipts.isEmpty()) {
            return StoreReceivingStatus.NOT_STARTED;
        }
        if (receipts.stream().allMatch(r -> r.getStatus() == StoreReceiptStatus.COMPLETED)) {
            return StoreReceivingStatus.COMPLETED;
        }
        if (receipts.stream().allMatch(r -> r.getStatus() == StoreReceiptStatus.INSPECTED
                || r.getStatus() == StoreReceiptStatus.CONFIRMED)) {
            return StoreReceivingStatus.INSPECTED;
        }
        if (receipts.stream().allMatch(r -> r.getReceivedAt() != null)) {
            return StoreReceivingStatus.PACKAGES_RECEIVED;
        }
        return StoreReceivingStatus.RECEIVING;
    }

    /** F1 still open: truck arrived and "complete receiving" not done yet */
    public boolean canReceive() {
        StoreReceivingStatus status = receivingStatus();
        return deliveryArrived()
                && (status == StoreReceivingStatus.NOT_STARTED || status == StoreReceivingStatus.RECEIVING);
    }

    public PackageReceivingState packageState(Long packageId) {
        PackageReceivingChecks check = checkByPackageId.get(packageId);
        if (check == null) {
            return PackageReceivingState.NOT_RECEIVED;
        }
        return check.getInspectionMethod() == InspectionMethod.FULL_COUNT
                ? PackageReceivingState.CHECKED
                : PackageReceivingState.RECEIVED;
    }

    public Optional<Packages> findPackage(String packageCode) {
        return packages.stream().filter(p -> p.getPackagesCode().equals(packageCode)).findFirst();
    }

    public Optional<StoreReceipts> receiptOfTransfer(Long transferId) {
        return receipts.stream().filter(r -> r.getStockTransfer().getId().equals(transferId)).findFirst();
    }
}