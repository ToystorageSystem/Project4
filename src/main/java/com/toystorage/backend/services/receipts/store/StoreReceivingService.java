package com.toystorage.backend.services.receipts.store;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.dto.request.receipts.store.ConfirmReceivingRequest;
import com.toystorage.backend.dto.request.receipts.store.PackageCountRequest;
import com.toystorage.backend.dto.request.receipts.store.ScanPackageRequest;
import com.toystorage.backend.dto.response.receipts.store.ManifestDetailResponse;
import com.toystorage.backend.dto.response.receipts.store.ManifestSummaryResponse;
import com.toystorage.backend.dto.response.receipts.store.PackageCheckDetailResponse;
import com.toystorage.backend.dto.response.receipts.store.ReceivingResultResponse;
import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.inventories.DiscrepancyItems;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.PackageReceivingChecks;
import com.toystorage.backend.entity.receipts.StorePackageItemChecks;
import com.toystorage.backend.entity.receipts.StoreReceiptItems;
import com.toystorage.backend.entity.receipts.StoreReceipts;
import com.toystorage.backend.entity.stores.StoreStockTransactions;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancySeverity;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.receipts.InspectionMethod;
import com.toystorage.backend.enums.receipts.PackageCondition;
import com.toystorage.backend.enums.receipts.PackageReceivingState;
import com.toystorage.backend.enums.receipts.SealStatus;
import com.toystorage.backend.enums.receipts.StoreReceiptStatus;
import com.toystorage.backend.enums.receipts.StoreReceivingStatus;
import com.toystorage.backend.enums.stores.StockTransactionType;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.receipts.store.StoreReceivingMapper;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyItemRepository;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.store.PackageReceivingCheckRepository;
import com.toystorage.backend.repository.receipts.store.StorePackageItemCheckRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceiptItemRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceiptRepository;
import com.toystorage.backend.repository.receipts.store.StoreReceivingQueryRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockTransactionRepository;
import com.toystorage.backend.services.inventories.discrepancy.DiscrepancySeverityPolicy;
import com.toystorage.backend.services.stores.StoreAccessService;
import com.toystorage.backend.services.stores.StoreInventorySyncService;

import lombok.RequiredArgsConstructor;

/**
 * Store receiving by manifest.
 * F1 = receive packages (seal + outside condition), F2 = count products inside each received package,
 * CONFIRM = Store Manager adds the stock, updates the transfer and reports discrepancies.
 * Writes lock the manifest row and use READ_COMMITTED so two staff never create duplicate receipts.
 */
@Service
@RequiredArgsConstructor
public class StoreReceivingService {

    /** deliveries the store can see in its list */
    private static final List<DeliveryStatus> VISIBLE_DELIVERY_STATUSES =
            List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED, DeliveryStatus.DELIVERED);

    private final StoreAccessService storeAccessService;
    private final StoreReceivingQueryRepository queryRepository;
    private final StoreManifestLoader loader;
    private final StoreReceivingMapper mapper;

    private final StoreReceiptRepository receiptRepository;
    private final StoreReceiptItemRepository receiptItemRepository;
    private final PackageReceivingCheckRepository checkRepository;
    private final StorePackageItemCheckRepository itemCheckRepository;

    private final StoreStockRepository stockRepository;
    private final StoreStockTransactionRepository stockTransactionRepository;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final DiscrepancySeverityPolicy severityPolicy;
    private final DiscrepancyItemRepository discrepancyItemRepository;
    private final StoreInventorySyncService storeInventorySyncService;

    // =====================================================================
    // READ
    // =====================================================================

    /** GET /api/store/receiving/manifests – incoming manifests not completed yet */
    @Transactional(readOnly = true)
    public List<ManifestSummaryResponse> listManifests(Long userId) {
        Long storeId = storeAccessService.requireStoreId(userId);
        return queryRepository.findStoreDeliveries(storeId, VISIBLE_DELIVERY_STATUSES).stream()
                .map(loader::load)
                .filter(data -> data.receivingStatus() != StoreReceivingStatus.COMPLETED)
                .map(mapper::toSummary)
                .toList();
    }

    /** GET /api/store/receiving/manifests/{manifestCode} */
    @Transactional(readOnly = true)
    public ManifestDetailResponse getManifest(Long userId, String manifestCode) {
        Long storeId = storeAccessService.requireStoreId(userId);
        return mapper.toDetail(loader.load(storeId, manifestCode));
    }

    // =====================================================================
    // F1 – receive packages (quick, the driver can leave)
    // =====================================================================

    /** POST /api/store/receiving/manifests/{manifestCode}/packages/scan */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ManifestDetailResponse scanPackage(Long userId, String manifestCode, ScanPackageRequest request) {
        Users user = storeAccessService.requireStoreUser(userId);
        StoreManifestData data = lockAndLoad(user.getWarehouse().getId(), manifestCode);

        if (!data.deliveryArrived()) {
            throw new BadRequest("Delivery has not arrived yet");
        }
        if (!data.canReceive()) {
            throw new BadRequest("Package receiving is already completed");
        }

        String packageCode = request.packageCode().trim();
        Packages pkg = data.findPackage(packageCode)
                .orElseThrow(() -> new BadRequest("Package does not belong to this manifest"));
        if (data.checkByPackageId().containsKey(pkg.getId())) {
            throw new BadRequest("Package has already been received");
        }

        String notes = normalize(request.notes());
        boolean abnormal = request.sealStatus() != SealStatus.INTACT
                || request.packageCondition() != PackageCondition.GOOD;
        if (abnormal && notes == null) {
            throw new BadRequest("Notes are required for an abnormal package");
        }

        StockTransfer transfer = data.transferByPackageId().get(pkg.getId());
        if (transfer == null) {
            throw new BadRequest("Package is not linked to any transfer");
        }

        // first scan of this manifest -> create 1 receipt (+ lines) per transfer
        if (data.receipts().isEmpty()) {
            startReceiving(data, user);
            data = loader.load(data.delivery());
        }
        StoreReceipts receipt = data.receiptOfTransfer(transfer.getId())
                .orElseThrow(() -> new BadRequest("Transfer is not part of this manifest"));

        checkRepository.save(PackageReceivingChecks.builder()
                .storeReceipt(receipt)
                .packageEntity(pkg)
                .sealStatus(request.sealStatus())
                .packageCondition(request.packageCondition())
                .inspectionMethod(InspectionMethod.SEAL_ONLY)   // F2 will change it to FULL_COUNT
                .checkedBy(user)
                .checkedAt(LocalDateTime.now())
                .notes(notes)
                .packageReceivingChecksCode("PRC-" + receipt.getId() + "-" + pkg.getId())
                .build());

        pkg.setStatus(PackageStatus.RECEIVED);                  // managed entity -> saved on commit

        return mapper.toDetail(loader.load(data.delivery()));
    }

    /** POST /api/store/receiving/manifests/{manifestCode}/complete-receiving */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ManifestDetailResponse completeReceiving(Long userId, String manifestCode) {
        Users user = storeAccessService.requireStoreUser(userId);
        StoreManifestData data = lockAndLoad(user.getWarehouse().getId(), manifestCode);

        if (!data.deliveryArrived()) {
            throw new BadRequest("Delivery has not arrived yet");
        }
        StoreReceivingStatus status = data.receivingStatus();
        if (status == StoreReceivingStatus.NOT_STARTED || data.checkByPackageId().isEmpty()) {
            throw new BadRequest("No package has been received");
        }
        if (status != StoreReceivingStatus.RECEIVING) {
            throw new BadRequest("Package receiving is already completed");
        }

        // packages not scanned now = missing packages (counted as shortage at finish inspection)
        LocalDateTime now = LocalDateTime.now();
        for (StoreReceipts receipt : data.receipts()) {
            receipt.setReceivedAt(now);
            receipt.setReceivedBy(user);
        }

        // finishing F1 with the driver present = handover signed -> trip DELIVERED (2026-11-13)
        Deliveries delivery = data.delivery();
        if (delivery.getDeliveryStatus() == DeliveryStatus.ARRIVED) {
            delivery.setDeliveryStatus(DeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(now);
            delivery.setUpdatedAt(now);
        }
        return mapper.toDetail(loader.load(data.delivery()));
    }

    // =====================================================================
    // F2 – count products inside each received package
    // =====================================================================

    /** GET /api/store/receiving/packages/{packageCode}/items */
    @Transactional(readOnly = true)
    public PackageCheckDetailResponse getPackageItems(Long userId, String packageCode) {
        Long storeId = storeAccessService.requireStoreId(userId);
        Packages pkg = findPackage(packageCode);
        StoreManifestData data = loadByPackage(storeId, pkg);
        requireReceived(data, pkg);
        return toPackageCheckDetail(data, pkg);
    }

    /** PUT /api/store/receiving/packages/{packageCode}/items – replaces previous counts of the package */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PackageCheckDetailResponse savePackageCounts(Long userId, String packageCode,
                                                       PackageCountRequest request) {
        Users user = storeAccessService.requireStoreUser(userId);
        Packages pkg = findPackage(packageCode);
        StoreManifestData first = loadByPackage(user.getWarehouse().getId(), pkg);
        queryRepository.lockManifest(first.manifest().getId());
        StoreManifestData data = loader.load(first.delivery());

        requireReceived(data, pkg);
        StoreReceivingStatus status = data.receivingStatus();
        if (status == StoreReceivingStatus.INSPECTED || status == StoreReceivingStatus.COMPLETED) {
            throw new BadRequest("Inspection is already finished");
        }

        // --- validate the lines (everything is checked BEFORE old counts are deleted) ---
        Map<String, PackageCountRequest.Line> lineByBarcode = new LinkedHashMap<>();
        for (PackageCountRequest.Line line : request.lines()) {
            String barcode = line.barcode().trim();
            if (line.damagedQuantity() > line.actualQuantity()) {
                throw new BadRequest("Damaged quantity cannot exceed actual quantity: " + barcode);
            }
            if (lineByBarcode.put(barcode, line) != null) {
                throw new BadRequest("Duplicate barcode in lines: " + barcode);
            }
        }

        // Barcode co the duoc nhieu NCC dung chung; phai lay Product tu chung tu Package.
        Map<Long, Integer> expectedByProductId = data.itemsByPackageId().getOrDefault(pkg.getId(), List.of())
                .stream().collect(Collectors.toMap(pi -> pi.getProduct().getId(),
                        PackageItems::getQuantity, Integer::sum));
        Map<String, List<Products>> candidatesByBarcode = queryRepository
                .findProductsByBarcodes(lineByBarcode.keySet())
                .stream().collect(Collectors.groupingBy(Products::getBarcode));
        Map<String, Products> productByBarcode = new LinkedHashMap<>();
        for (String barcode : lineByBarcode.keySet()) {
            List<Products> candidates = candidatesByBarcode.getOrDefault(barcode, List.of());
            if (candidates.isEmpty()) throw new NotFound("Product not found: " + barcode);
            List<Products> expected = candidates.stream()
                    .filter(p -> expectedByProductId.containsKey(p.getId())).toList();
            if (expected.size() != 1) {
                throw new BadRequest("Barcode cannot be resolved uniquely for this package: " + barcode);
            }
            productByBarcode.put(barcode, expected.get(0));
        }
        Set<Long> countedIds = productByBarcode.values().stream()
                .map(Products::getId).collect(Collectors.toSet());
        if (!countedIds.containsAll(expectedByProductId.keySet())) {
            throw new BadRequest("All products in the package must be counted");
        }

        // --- replace the counts of this package ---
        PackageReceivingChecks check = data.checkByPackageId().get(pkg.getId());
        StoreReceipts receipt = check.getStoreReceipt();
        LocalDateTime now = LocalDateTime.now();

        itemCheckRepository.deleteByPackageId(pkg.getId());
        List<StorePackageItemChecks> counts = lineByBarcode.entrySet().stream()
                .map(e -> {
                    Products product = productByBarcode.get(e.getKey());
                    PackageCountRequest.Line line = e.getValue();
                    return StorePackageItemChecks.builder()
                            .storePackageItemChecksCode("SPIC-" + pkg.getId() + "-" + product.getId())
                            .storeReceipt(receipt)
                            .packageEntity(pkg)
                            .product(product)
                            .expectedQuantity(expectedByProductId.getOrDefault(product.getId(), 0))
                            .actualQuantity(line.actualQuantity())
                            .damagedQuantity(line.damagedQuantity())
                            .note(normalize(line.note()))
                            .checkedBy(user)
                            .checkedAt(now)
                            .build();
                })
                .toList();
        itemCheckRepository.saveAll(counts);

        check.setInspectionMethod(InspectionMethod.FULL_COUNT);   // package -> CHECKED

        return toPackageCheckDetail(loader.load(data.delivery()), pkg);
    }

    /** POST /api/store/receiving/manifests/{manifestCode}/finish-inspection */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReceivingResultResponse finishInspection(Long userId, String manifestCode) {
        Users user = storeAccessService.requireStoreUser(userId);
        StoreManifestData data = lockAndLoad(user.getWarehouse().getId(), manifestCode);

        StoreReceivingStatus status = data.receivingStatus();
        if (status == StoreReceivingStatus.NOT_STARTED || status == StoreReceivingStatus.RECEIVING) {
            throw new BadRequest("Package receiving is not completed");
        }
        if (status != StoreReceivingStatus.PACKAGES_RECEIVED) {
            throw new BadRequest("Inspection is already finished");
        }
        List<String> notChecked = data.packages().stream()
                .filter(p -> data.packageState(p.getId()) == PackageReceivingState.RECEIVED)
                .map(Packages::getPackagesCode)
                .toList();
        if (!notChecked.isEmpty()) {
            throw new BadRequest("All received packages must be checked: " + String.join(", ", notChecked));
        }

        List<Long> receiptIds = data.receipts().stream().map(StoreReceipts::getId).toList();

        // sum the package counts per (receipt, product)
        Map<String, int[]> totals = new HashMap<>();          // "receiptId-productId" -> [actual, damaged]
        Map<String, StorePackageItemChecks> anyCountOfKey = new HashMap<>();
        for (StorePackageItemChecks c : itemCheckRepository.findByStoreReceipt_IdIn(receiptIds)) {
            String key = c.getStoreReceipt().getId() + "-" + c.getProduct().getId();
            int[] t = totals.computeIfAbsent(key, k -> new int[2]);
            t[0] += c.getActualQuantity();
            t[1] += c.getDamagedQuantity();
            anyCountOfKey.putIfAbsent(key, c);
        }

        // existing receipt lines (expected from the transfer); missing packages -> 0 -> shortage
        List<StoreReceiptItems> items = new ArrayList<>(receiptItemRepository.findByStoreReceipt_IdIn(receiptIds));
        Set<String> keysWithLine = new HashSet<>();
        for (StoreReceiptItems item : items) {
            String key = item.getStoreReceipt().getId() + "-" + item.getProduct().getId();
            keysWithLine.add(key);
            int[] t = totals.getOrDefault(key, new int[2]);
            item.setActualQuantity(t[0]);
            item.setDamagedQuantity(t[1]);
            item.calculateDifference();                       // team helper: shortage / surplus
        }

        // surplus products that are not on the transfer at all -> new line, expected 0
        for (Map.Entry<String, int[]> e : totals.entrySet()) {
            if (keysWithLine.contains(e.getKey())) {
                continue;
            }
            StorePackageItemChecks c = anyCountOfKey.get(e.getKey());
            StoreReceiptItems extra = StoreReceiptItems.builder()
                    .storeReceipt(c.getStoreReceipt())
                    .product(c.getProduct())
                    .expectedQuantity(0)
                    .actualQuantity(e.getValue()[0])
                    .damagedQuantity(e.getValue()[1])
                    .storeReceiptItemsCode("SRCPI-" + c.getStoreReceipt().getId() + "-" + c.getProduct().getId())
                    .build();
            extra.calculateDifference();
            items.add(receiptItemRepository.save(extra));
        }

        data.receipts().forEach(r -> r.setStatus(StoreReceiptStatus.INSPECTED));

        // common rule (2026-11-13): nothing missing / extra / damaged -> completed without the manager
        if (items.stream().noneMatch(StoreReceivingService::hasDiscrepancy)) {
            applyConfirm(data, user.getWarehouse().getId(), user, null, items);
            return mapper.toResult(data.manifest().getManifestCode(), StoreReceivingStatus.COMPLETED,
                    items, List.of());
        }
        return mapper.toResult(data.manifest().getManifestCode(), StoreReceivingStatus.INSPECTED,
                items, List.of());
    }

    // =====================================================================
    // CONFIRM – Store Manager only: add stock, update the transfer, report discrepancies
    // =====================================================================

    /** POST /api/store/receiving/manifests/{manifestCode}/confirm */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReceivingResultResponse confirm(Long userId, String manifestCode, ConfirmReceivingRequest request) {
        Users manager = storeAccessService.requireStoreManager(userId);
        Long storeId = manager.getWarehouse().getId();
        StoreManifestData data = lockAndLoad(storeId, manifestCode);

        StoreReceivingStatus status = data.receivingStatus();
        if (status == StoreReceivingStatus.COMPLETED) {
            throw new BadRequest("Receiving is already completed");
        }
        if (status != StoreReceivingStatus.INSPECTED) {
            throw new BadRequest("Inspection is not finished");
        }

        String note = request == null ? null : normalize(request.note());
        List<Long> receiptIds = data.receipts().stream().map(StoreReceipts::getId).toList();
        List<StoreReceiptItems> items = receiptItemRepository.findByStoreReceipt_IdIn(receiptIds);

        List<String> reportCodes = applyConfirm(data, storeId, manager, note, items);
        return mapper.toResult(data.manifest().getManifestCode(), StoreReceivingStatus.COMPLETED,
                items, reportCodes);
    }

    /**
     * Adds the stock and closes the receipts. Used by the manager's confirm and by finish-inspection
     * when there is no discrepancy.
     * - transfer without discrepancy -> COMPLETED, receipt COMPLETED
     * - transfer with discrepancy -> RECEIVED, receipt CONFIRMED, reports sent to Business;
     *   Business resolving the last report completes the transfer.
     */
    private List<String> applyConfirm(StoreManifestData data, Long storeId, Users manager, String note,
                                      List<StoreReceiptItems> items) {
        LocalDateTime now = LocalDateTime.now();

        // ---- 1. store stock: backroom += actual - damaged, + RECEIVE history ----
        for (StoreReceiptItems item : items) {
            int accepted = item.getAcceptedQuantity();          // team helper: actual - damaged
            if (accepted <= 0) {
                continue;
            }
            StoreStocks stock = stockRepository.findByStoreIdAndProductId(storeId, item.getProduct().getId())
                    .orElseGet(() -> StoreStocks.newRow(storeId, item.getProduct()));
            stock.addToBackroom(accepted);
            stockRepository.save(stock);
            storeInventorySyncService.sync(stock);              // total -> inventory_balances

            StoreReceipts receipt = item.getStoreReceipt();
            stockTransactionRepository.save(StoreStockTransactions.builder()
                    .code("STX-R" + receipt.getId() + "-" + item.getProduct().getId())
                    .storeId(storeId)
                    .product(item.getProduct())
                    .type(StockTransactionType.RECEIVE)
                    .shelfChange(0)
                    .backroomChange(accepted)
                    .shelfAfter(stock.getShelfQuantity())
                    .backroomAfter(stock.getBackroomQuantity())
                    .storeReceipt(receipt)
                    .note("Received " + receipt.getStoreReceiptsCode())
                    .createdBy(manager)
                    .createdAt(now)
                    .build());
        }

        // ---- 2. transfer lines + transfer status (warehouse side can see the result) ----
        List<Long> transferIds = data.transfers().stream().map(StockTransfer::getId).toList();
        Map<String, StoreReceiptItems> itemByTransferProduct = items.stream().collect(Collectors.toMap(
                i -> i.getStoreReceipt().getStockTransfer().getId() + "-" + i.getProduct().getId(),
                Function.identity(), (a, b) -> a));
        for (StockTransferItems sti : queryRepository.findTransferItems(transferIds)) {
            StoreReceiptItems item = itemByTransferProduct.get(
                    sti.getStockTransfer().getId() + "-" + sti.getProduct().getId());
            if (item != null) {
                sti.setReceivedQuantity(item.getActualQuantity());
                sti.setShortageQuantity(item.getShortageQuantity());
                sti.setSurplusQuantity(item.getSurplusQuantity());
            }
        }
        Set<Long> transfersWithDiscrepancy = items.stream()
                .filter(StoreReceivingService::hasDiscrepancy)
                .map(i -> i.getStoreReceipt().getStockTransfer().getId())
                .collect(Collectors.toSet());
        for (StockTransfer transfer : data.transfers()) {
            transfer.setStatus(transfersWithDiscrepancy.contains(transfer.getId())
                    ? TransferStatus.RECEIVED          // waits for Business to resolve the reports
                    : TransferStatus.COMPLETED);
            transfer.setReceivedAt(now);
        }

        // ---- 3. discrepancy reports (same style as the team's ReceivingDiscrepancyService) ----
        List<String> reportCodes = new ArrayList<>();
        for (StoreReceiptItems item : items) {
            addReport(reportCodes, item, DiscrepancyType.SHORTAGE, item.getShortageQuantity(), manager, note);
            addReport(reportCodes, item, DiscrepancyType.SURPLUS, item.getSurplusQuantity(), manager, note);
            addReport(reportCodes, item, DiscrepancyType.DAMAGED, item.getDamagedQuantity(), manager, note);
        }

        // ---- 4. receipts done (CONFIRMED = Business still has to resolve the reports) ----
        for (StoreReceipts receipt : data.receipts()) {
            receipt.setStatus(transfersWithDiscrepancy.contains(receipt.getStockTransfer().getId())
                    ? StoreReceiptStatus.CONFIRMED
                    : StoreReceiptStatus.COMPLETED);
            receipt.setConfirmedBy(manager);
        }
        return reportCodes;
    }

    private static boolean hasDiscrepancy(StoreReceiptItems item) {
        return positive(item.getShortageQuantity()) || positive(item.getSurplusQuantity())
                || positive(item.getDamagedQuantity());
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }

    // =====================================================================
    // helpers
    // =====================================================================

    /** one discrepancy report per product per type (only when quantity > 0) */
    private void addReport(List<String> codes, StoreReceiptItems item, DiscrepancyType type,
                           Integer quantity, Users reporter, String managerNote) {
        if (quantity == null || quantity <= 0) {
            return;
        }
        StoreReceipts receipt = item.getStoreReceipt();
        DiscrepancySeverity severity = severityPolicy.determine(
                item.getExpectedQuantity(), item.getActualQuantity(),
                item.getDamagedQuantity() == null ? 0 : item.getDamagedQuantity());
        String code = "DR-SR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String description = type + " " + quantity + " x " + item.getProduct().getProductsCode()
                + " (expected " + item.getExpectedQuantity() + ", actual " + item.getActualQuantity()
                + ", damaged " + item.getDamagedQuantity() + ") - store receipt "
                + receipt.getStoreReceiptsCode() + ", transfer " + receipt.getStockTransfer().getTransferCode()
                + (managerNote == null ? "" : ". Note: " + managerNote);

        // Business resolves store receiving discrepancies as STOCK_TRANSFER reports (BusinessManagerDiscrepancyService)
        DiscrepancyReports report = discrepancyReportRepository.save(DiscrepancyReports.builder()
                .reportCode(code)
                .discrepancyReportsCode(code)
                .referenceType(DiscrepancyReferenceType.STOCK_TRANSFER)
                .referenceId(receipt.getStockTransfer().getId())
                .productId(item.getProduct().getId())
                .warehouse(receipt.getStore())
                .discrepancyType(type)
                .severity(severity)
                .blocking(severityPolicy.isBlocking(severity))
                .status(DiscrepancyStatus.INVESTIGATING)
                .reportedBy(reporter)
                .responsibleParty("BUSINESS_MANAGER")
                .description(description)
                .build());

        int expected = item.getExpectedQuantity() == null ? 0 : item.getExpectedQuantity();
        int actual = item.getActualQuantity() == null ? 0 : item.getActualQuantity();
        discrepancyItemRepository.save(DiscrepancyItems.builder()
                .discrepancyReport(report)
                .product(item.getProduct())
                .expectedQuantity(expected)
                .actualQuantity(actual)
                .differenceQuantity(type == DiscrepancyType.DAMAGED ? -quantity : actual - expected)
                .discrepancyItemsCode("DI-" + code.substring(3))
                .build());
        codes.add(code);
    }

    /** lock the manifest row first, then read the latest committed data (READ_COMMITTED) */
    private StoreManifestData lockAndLoad(Long storeId, String manifestCode) {
        StoreManifestData first = loader.load(storeId, manifestCode);      // 404 if not my manifest
        queryRepository.lockManifest(first.manifest().getId());
        return loader.load(first.delivery());
    }

    /** 1 receipt per transfer of the manifest, lines expected = shipped quantity */
    private void startReceiving(StoreManifestData data, Users user) {
        List<Long> transferIds = data.transfers().stream().map(StockTransfer::getId).toList();
        Map<Long, List<StockTransferItems>> itemsByTransfer = queryRepository.findTransferItems(transferIds)
                .stream().collect(Collectors.groupingBy(sti -> sti.getStockTransfer().getId()));

        for (StockTransfer transfer : data.transfers()) {
            StoreReceipts receipt = receiptRepository.save(StoreReceipts.builder()
                    .stockTransfer(transfer)
                    .store(user.getWarehouse())
                    .status(StoreReceiptStatus.RECEIVING)
                    .storeReceiptsCode("SRCP-T" + transfer.getId())
                    .build());

            List<StoreReceiptItems> lines = itemsByTransfer.getOrDefault(transfer.getId(), List.of()).stream()
                    .map(sti -> StoreReceiptItems.builder()
                            .storeReceipt(receipt)
                            .product(sti.getProduct())
                            .expectedQuantity(sti.getShippedQuantity())
                            .storeReceiptItemsCode("SRCPI-" + receipt.getId() + "-" + sti.getProduct().getId())
                            .build())
                    .toList();
            receiptItemRepository.saveAll(lines);

            if (transfer.getStatus() == TransferStatus.SHIPPED) {
                transfer.setStatus(TransferStatus.RECEIVING);   // "destination is checking the goods"
            }
        }
    }

    private Packages findPackage(String packageCode) {
        String code = normalize(packageCode);
        if (code == null) {
            throw new NotFound("Package not found");
        }
        return queryRepository.findPackageByCode(code)
                .orElseThrow(() -> new NotFound("Package not found"));
    }

    /** manifest data of the package, only if the manifest goes to MY store */
    private StoreManifestData loadByPackage(Long storeId, Packages pkg) {
        Deliveries delivery = queryRepository.findStoreDeliveriesByPackageId(storeId, pkg.getId())
                .stream().findFirst()
                .orElseThrow(() -> new NotFound("Package not found"));
        return loader.load(delivery);
    }

    private static void requireReceived(StoreManifestData data, Packages pkg) {
        if (data.packageState(pkg.getId()) == PackageReceivingState.NOT_RECEIVED) {
            throw new BadRequest("Package has not been received yet");
        }
    }

    private PackageCheckDetailResponse toPackageCheckDetail(StoreManifestData data, Packages pkg) {
        return mapper.toPackageCheckDetail(
                pkg,
                data.manifest().getManifestCode(),
                data.packageState(pkg.getId()),
                data.itemsByPackageId().getOrDefault(pkg.getId(), List.of()),
                itemCheckRepository.findByPackageEntity_Id(pkg.getId()));
    }

    /** trim; blank -> null */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}