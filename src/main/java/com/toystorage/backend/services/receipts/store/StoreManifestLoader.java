package com.toystorage.backend.services.receipts.store;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.receipts.PackageReceivingChecks;
import com.toystorage.backend.entity.receipts.StoreReceipts;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.receipts.store.PackageReceivingCheckRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceiptRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceivingQueryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Loads a StoreManifestData. Must be called inside a @Transactional service method.
 * About 6 small queries per manifest, no N+1.
 */
@Component
@RequiredArgsConstructor
public class StoreManifestLoader {

    private final StoreReceivingQueryRepository queryRepository;
    private final StoreReceiptRepository receiptRepository;
    private final PackageReceivingCheckRepository checkRepository;

    /** manifest of MY store, else 404 (never reveal other stores' manifests) */
    public StoreManifestData load(Long storeId, String manifestCode) {
        String code = manifestCode == null ? "" : manifestCode.trim();
        Deliveries delivery = queryRepository.findStoreDeliveriesByManifestCode(storeId, code)
                .stream().findFirst()
                .orElseThrow(() -> new NotFound("Manifest not found"));
        return load(delivery);
    }

    public StoreManifestData load(Deliveries delivery) {
        ShipmentManifests manifest = delivery.getManifest();

        List<Packages> packages = queryRepository.findPackagesOfManifest(manifest.getId());
        List<StockTransfer> transfers = queryRepository.findTransfersOfManifest(manifest.getId());

        List<Long> packageIds = packages.stream().map(Packages::getId).toList();
        Map<Long, StockTransfer> transferByPackageId = packageIds.isEmpty() ? Map.of()
                : queryRepository.findPackageTransfers(packageIds).stream()
                        .collect(Collectors.toMap(pti -> pti.getPackageEntity().getId(),
                                PackageTransferItem::getStockTransfer, (a, b) -> a));
        Map<Long, List<PackageItems>> itemsByPackageId = packageIds.isEmpty() ? Map.of()
                : queryRepository.findPackageItems(packageIds).stream()
                        .collect(Collectors.groupingBy(pi -> pi.getPackageEntity().getId()));

        List<Long> transferIds = transfers.stream().map(StockTransfer::getId).toList();
        List<StoreReceipts> receipts = transferIds.isEmpty() ? List.of()
                : receiptRepository.findByStockTransfer_IdIn(transferIds);

        List<Long> receiptIds = receipts.stream().map(StoreReceipts::getId).toList();
        Map<Long, PackageReceivingChecks> checkByPackageId = receiptIds.isEmpty() ? Map.of()
                : checkRepository.findByStoreReceipt_IdIn(receiptIds).stream()
                        .collect(Collectors.toMap(c -> c.getPackageEntity().getId(),
                                Function.identity(), (a, b) -> a));

        return new StoreManifestData(delivery, manifest, packages, transfers,
                transferByPackageId, itemsByPackageId, receipts, checkByPackageId);
    }
}