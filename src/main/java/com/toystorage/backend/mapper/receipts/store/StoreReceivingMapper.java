package com.toystorage.backend.mapper.receipts.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.toystorage.backend.dto.response.receipts.store.ManifestDetailResponse;
import com.toystorage.backend.dto.response.receipts.store.ManifestSummaryResponse;
import com.toystorage.backend.dto.response.receipts.store.PackageCheckDetailResponse;
import com.toystorage.backend.dto.response.receipts.store.PackageCheckLineResponse;
import com.toystorage.backend.dto.response.receipts.store.PackageStatusResponse;
import com.toystorage.backend.dto.response.receipts.store.ReceiptBriefResponse;
import com.toystorage.backend.dto.response.receipts.store.ReceivingResultItemResponse;
import com.toystorage.backend.dto.response.receipts.store.ReceivingResultResponse;
import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.PackageReceivingChecks;
import com.toystorage.backend.entity.receipts.StorePackageItemChecks;
import com.toystorage.backend.entity.receipts.StoreReceiptItems;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.enums.receipts.PackageReceivingState;
import com.toystorage.backend.enums.receipts.StoreReceivingStatus;
import com.toystorage.backend.services.receipts.store.StoreManifestData;

@Component
public class StoreReceivingMapper {

    // =====================================================================
    // manifest list / detail
    // =====================================================================

    public ManifestSummaryResponse toSummary(StoreManifestData data) {
        Deliveries delivery = data.delivery();
        ShipmentManifests manifest = data.manifest();

        int received = 0;
        int checked = 0;
        for (Packages p : data.packages()) {
            PackageReceivingState state = data.packageState(p.getId());
            if (state != PackageReceivingState.NOT_RECEIVED) {
                received++;
            }
            if (state == PackageReceivingState.CHECKED) {
                checked++;
            }
        }

        return new ManifestSummaryResponse(
                manifest.getId(),
                manifest.getManifestCode(),
                delivery.getShipmentCode(),
                delivery.getDeliveryStatus().name(),
                delivery.getFromWarehouse().getName(),
                delivery.getExpectedDeliveryAt(),
                data.packages().size(),
                received,
                checked,
                data.receivingStatus(),
                data.canReceive());
    }

    public ManifestDetailResponse toDetail(StoreManifestData data) {
        List<PackageStatusResponse> packages = data.packages().stream()
                .map(p -> toPackageStatus(data, p))
                .toList();

        List<ReceiptBriefResponse> receipts = data.receipts().stream()
                .map(r -> new ReceiptBriefResponse(
                        r.getId(),
                        r.getStoreReceiptsCode(),
                        r.getStockTransfer().getTransferCode(),
                        r.getStatus(),
                        r.getReceivedAt()))
                .toList();

        return new ManifestDetailResponse(toSummary(data), packages, receipts);
    }

    private PackageStatusResponse toPackageStatus(StoreManifestData data, Packages p) {
        PackageReceivingChecks check = data.checkByPackageId().get(p.getId());
        StockTransfer transfer = data.transferByPackageId().get(p.getId());
        List<PackageItems> items = data.itemsByPackageId().getOrDefault(p.getId(), List.of());
        int pieces = items.stream().mapToInt(PackageItems::getQuantity).sum();

        return new PackageStatusResponse(
                p.getId(),
                p.getPackagesCode(),
                p.getSealNumber(),
                transfer == null ? null : transfer.getTransferCode(),
                data.packageState(p.getId()),
                check == null ? null : check.getSealStatus(),
                check == null ? null : check.getPackageCondition(),
                check == null ? null : check.getNotes(),
                items.size(),
                pieces);
    }

    // =====================================================================
    // F2 – package count / inspection result
    // =====================================================================

    /** expected lines (package_items) merged with the store counts; extra counted products = surplus lines */
    public PackageCheckDetailResponse toPackageCheckDetail(Packages pkg, String manifestCode,
                                                           PackageReceivingState state,
                                                           List<PackageItems> expectedItems,
                                                           List<StorePackageItemChecks> counts) {
        Map<Long, StorePackageItemChecks> countByProductId = counts.stream()
                .collect(Collectors.toMap(c -> c.getProduct().getId(), Function.identity(), (a, b) -> a));

        List<PackageCheckLineResponse> lines = new ArrayList<>();
        Set<Long> expectedIds = new HashSet<>();
        for (PackageItems item : expectedItems) {
            Products product = item.getProduct();
            expectedIds.add(product.getId());
            StorePackageItemChecks count = countByProductId.get(product.getId());
            lines.add(new PackageCheckLineResponse(
                    product.getId(),
                    product.getProductsCode(),
                    product.getBarcode(),
                    product.getName(),
                    item.getQuantity(),
                    count == null ? null : count.getActualQuantity(),
                    count == null ? null : count.getDamagedQuantity(),
                    count == null ? null : count.getNote()));
        }
        for (StorePackageItemChecks count : counts) {           // surplus: not expected in the package
            if (!expectedIds.contains(count.getProduct().getId())) {
                Products product = count.getProduct();
                lines.add(new PackageCheckLineResponse(
                        product.getId(),
                        product.getProductsCode(),
                        product.getBarcode(),
                        product.getName(),
                        0,
                        count.getActualQuantity(),
                        count.getDamagedQuantity(),
                        count.getNote()));
            }
        }
        return new PackageCheckDetailResponse(pkg.getId(), pkg.getPackagesCode(), manifestCode, state, lines);
    }

    public ReceivingResultResponse toResult(String manifestCode, StoreReceivingStatus status,
                                            List<StoreReceiptItems> items, List<String> discrepancyReportCodes) {
        List<ReceivingResultItemResponse> rows = items.stream()
                .map(i -> new ReceivingResultItemResponse(
                        i.getStoreReceipt().getStockTransfer().getTransferCode(),
                        i.getProduct().getId(),
                        i.getProduct().getProductsCode(),
                        i.getProduct().getName(),
                        i.getExpectedQuantity(),
                        i.getActualQuantity(),
                        i.getDamagedQuantity(),
                        i.getShortageQuantity(),
                        i.getSurplusQuantity()))
                .sorted(Comparator.comparing(ReceivingResultItemResponse::transferCode)
                        .thenComparing(ReceivingResultItemResponse::productCode))
                .toList();

        return new ReceivingResultResponse(
                manifestCode,
                status,
                rows,
                rows.stream().mapToInt(ReceivingResultItemResponse::expected).sum(),
                rows.stream().mapToInt(ReceivingResultItemResponse::actual).sum(),
                rows.stream().mapToInt(ReceivingResultItemResponse::damaged).sum(),
                rows.stream().mapToInt(ReceivingResultItemResponse::shortage).sum(),
                rows.stream().mapToInt(ReceivingResultItemResponse::surplus).sum(),
                discrepancyReportCodes);
    }
}