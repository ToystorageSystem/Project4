package com.toystorage.backend.services.inventories.lowstock;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.LowStockReport;
import com.toystorage.backend.entity.inventories.LowStockReportItem;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.warehouses.LowStockReportStatus;
import com.toystorage.backend.enums.warehouses.LowStockSeverity;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.lowstock.LowStockReportRepository;
import com.toystorage.backend.services.warehouses.manager.ManagerOpsAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/**
 * Bao cao hang ton kho sap het:
 *  - Manager xem danh sach san pham co ton kha dung <= muc toi thieu (minimum_stock_level).
 *  - Neu san pham chua dat muc toi thieu (=0) thi dung nguong mac dinh (threshold).
 *  - San pham ACTIVE chua co ton tai kho duoc coi la het hang.
 *  - Manager gui bao cao cho Business; Business tiep nhan / xu ly.
 */
@Service
@RequiredArgsConstructor
public class LowStockReportService {

    public static final int DEFAULT_THRESHOLD = 10;

    @PersistenceContext
    private EntityManager em;

    private final InventoryBalanceRepository balanceRepository;
    private final LowStockReportRepository reportRepository;
    private final ManagerOpsAccess access;

    // ===================== MANAGER =====================

    @Transactional(readOnly = true)
    public LowStockPreview preview(Integer threshold) {
        Users manager = access.currentWarehouseUser();
        return compute(manager.getWarehouse(), normalizeThreshold(threshold), true);
    }

    @Transactional(readOnly = true)
    public LowStockPreview allProducts(Integer threshold) {
        Users manager = access.currentWarehouseUser();
        return compute(manager.getWarehouse(), normalizeThreshold(threshold), false);
    }

    /** Dat muc ton toi thieu cho san pham tai kho cua manager (ghi vao moi dong ton cua san pham). */
    @Transactional
    public LowStockLine updateMinimum(Long productId, MinimumRequest request) {
        if (request == null || request.minimum() == null || request.minimum() < 0 || request.minimum() > 1_000_000) {
            throw bad("Minimum level must be between 0 and 1,000,000");
        }
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        List<InventoryBalances> rows = balanceRepository.findByWarehouseId(w).stream()
                .filter(b -> b.getProduct() != null && productId.equals(b.getProduct().getId()))
                .toList();
        if (rows.isEmpty()) {
            throw bad("This product has no stock location in your warehouse yet. Receive or put it away first.");
        }
        rows.forEach(b -> b.setMinimumStockLevel(request.minimum()));
        balanceRepository.saveAll(rows);
        return toLine(rows.get(0).getProduct(), rows, DEFAULT_THRESHOLD);
    }

    @Transactional
    public LowStockReportDetail send(SendLowStockRequest request) {
        Users manager = access.currentWarehouseUser();
        int threshold = normalizeThreshold(request == null ? null : request.threshold());
        LowStockPreview preview = compute(manager.getWarehouse(), threshold, true);
        Set<Long> selected = request == null || request.productIds() == null || request.productIds().isEmpty()
                ? null : new HashSet<>(request.productIds());
        List<LowStockLine> lines = preview.items().stream()
                .filter(l -> selected == null || selected.contains(l.productId()))
                .toList();
        if (lines.isEmpty()) throw bad("There are no low-stock products to report.");

        LowStockReport report = LowStockReport.builder()
                .reportCode(nextCode())
                .warehouse(manager.getWarehouse())
                .createdBy(manager)
                .status(LowStockReportStatus.SENT)
                .managerNote(trim(request == null ? null : request.note(), 1000))
                .build();
        Map<Long, Integer> overrides = request == null || request.suggestedQuantities() == null
                ? Map.of() : request.suggestedQuantities();
        for (LowStockLine l : lines) {
            Integer override = overrides.get(l.productId());
            int suggested = override != null && override > 0 ? override : l.suggestedQuantity();
            LowStockSeverity severity = LowStockSeverity.valueOf(l.severity());
            report.getItems().add(LowStockReportItem.builder()
                    .report(report)
                    .product(em.getReference(Products.class, l.productId()))
                    .productCode(l.productCode()).productName(l.productName())
                    .onHand((int) l.onHand()).reserved((int) l.reserved()).available((int) l.available())
                    .minimumLevel(l.minimumLevel()).suggestedQuantity(suggested)
                    .severity(severity).severityRank(severity.ordinal())
                    .build());
        }
        LowStockReport saved = reportRepository.save(report);
        return detailOf(saved);
    }

    @Transactional(readOnly = true)
    public List<LowStockReportSummary> managerReports() {
        Users manager = access.currentWarehouseUser();
        return reportRepository.findForWarehouse(manager.getWarehouse().getId()).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public LowStockReportDetail managerDetail(Long id) {
        Users manager = access.currentWarehouseUser();
        LowStockReport report = load(id);
        if (!report.getWarehouse().getId().equals(manager.getWarehouse().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
        }
        return detailOf(report);
    }

    // ===================== BUSINESS =====================

    @Transactional(readOnly = true)
    public List<LowStockReportSummary> businessReports(String status) {
        LowStockReportStatus filter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try { filter = LowStockReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw bad("Unknown status: " + status); }
        }
        return reportRepository.findAllForBusiness(filter).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public LowStockReportDetail businessDetail(Long id) { return detailOf(load(id)); }

    @Transactional
    public LowStockReportDetail acknowledge(Long id, BusinessHandleRequest request) {
        LowStockReport report = load(id);
        if (report.getStatus() != LowStockReportStatus.SENT) throw conflict("Only newly sent reports can be acknowledged.");
        report.setStatus(LowStockReportStatus.ACKNOWLEDGED);
        report.setAcknowledgedAt(LocalDateTime.now());
        report.setHandledBy(access.currentUser());
        if (request != null && request.note() != null && !request.note().isBlank()) report.setBusinessNote(trim(request.note(), 1000));
        return detailOf(reportRepository.save(report));
    }

    @Transactional
    public LowStockReportDetail resolve(Long id, BusinessHandleRequest request) {
        LowStockReport report = load(id);
        if (report.getStatus() == LowStockReportStatus.RESOLVED) throw conflict("This report is already resolved.");
        if (request == null || request.note() == null || request.note().trim().length() < 3) {
            throw bad("Enter the replenishment plan (purchase order, transfer…) – at least 3 characters.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (report.getAcknowledgedAt() == null) report.setAcknowledgedAt(now);
        report.setStatus(LowStockReportStatus.RESOLVED);
        report.setResolvedAt(now);
        report.setHandledBy(access.currentUser());
        report.setBusinessNote(trim(request.note(), 1000));
        return detailOf(reportRepository.save(report));
    }

    // ===================== INTERNAL =====================

    private LowStockPreview compute(Warehouses warehouse, int threshold, boolean onlyLow) {
        Map<Long, List<InventoryBalances>> byProduct = balanceRepository.findByWarehouseId(warehouse.getId()).stream()
                .filter(b -> b.getProduct() != null)
                .collect(Collectors.groupingBy(b -> b.getProduct().getId()));
        List<LowStockLine> lines = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (List<InventoryBalances> rows : byProduct.values()) {
            Products product = rows.get(0).getProduct();
            if (product.getStatus() != null && product.getStatus() != ProductStatus.ACTIVE) continue;
            seen.add(product.getId());
            lines.add(toLine(product, rows, threshold));
        }
        // San pham dang kinh doanh nhung chua tung co ton o kho nay.
        List<Products> active = em.createQuery("SELECT p FROM Products p WHERE p.status = :s", Products.class)
                .setParameter("s", ProductStatus.ACTIVE).getResultList();
        for (Products p : active) {
            if (!seen.contains(p.getId())) lines.add(toLine(p, List.of(), threshold));
        }
        if (onlyLow) lines.removeIf(l -> l.severity() == null);
        lines.sort(Comparator
                .comparing((LowStockLine l) -> l.severity() == null ? 9 : LowStockSeverity.valueOf(l.severity()).ordinal())
                .thenComparingLong(LowStockLine::available)
                .thenComparing(l -> l.productName() == null ? "" : l.productName().toLowerCase(Locale.ROOT)));
        long out = lines.stream().filter(l -> "OUT_OF_STOCK".equals(l.severity())).count();
        long critical = lines.stream().filter(l -> "CRITICAL".equals(l.severity())).count();
        long low = lines.stream().filter(l -> "LOW".equals(l.severity())).count();
        return new LowStockPreview(warehouse.getId(), warehouse.getName(), threshold, out, critical, low, lines);
    }

    private LowStockLine toLine(Products product, List<InventoryBalances> rows, int threshold) {
        long onHand = rows.stream().mapToLong(b -> safe(b.getQuantity())).sum();
        long reserved = rows.stream().mapToLong(b -> safe(b.getReservedQuantity())).sum();
        long available = rows.stream().mapToLong(b -> safe(b.getAvailableQuantity())).sum();
        int configured = rows.stream().mapToInt(b -> b.getMinimumStockLevel() == null ? 0 : b.getMinimumStockLevel())
                .max().orElse(0);
        boolean usesDefault = configured <= 0;
        int minimum = usesDefault ? threshold : configured;
        String severity = null;
        if (available <= 0) severity = LowStockSeverity.OUT_OF_STOCK.name();
        else if (available <= Math.max(1, minimum / 2)) severity = LowStockSeverity.CRITICAL.name();
        else if (available <= minimum) severity = LowStockSeverity.LOW.name();
        // Muc tieu bo sung = 2 x muc toi thieu.
        int suggested = (int) Math.max(0, (long) minimum * 2 - available);
        return new LowStockLine(product.getId(), product.getProductsCode(), product.getName(), product.getBarcode(),
                product.getBaseUnit(), onHand, reserved, available, minimum, usesDefault,
                severity == null ? 0 : Math.max(1, suggested), severity, rows.size());
    }

    private LowStockReport load(Long id) {
        return reportRepository.findDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private LowStockReportDetail detailOf(LowStockReport r) {
        List<LowStockReportItemView> items = r.getItems().stream()
                .sorted(Comparator.comparing(LowStockReportItem::getSeverityRank).thenComparing(LowStockReportItem::getAvailable))
                .map(i -> new LowStockReportItemView(i.getProduct().getId(), i.getProductCode(), i.getProductName(),
                        i.getOnHand(), i.getReserved(), i.getAvailable(), i.getMinimumLevel(),
                        i.getSuggestedQuantity(), i.getSeverity().name()))
                .toList();
        return new LowStockReportDetail(summary(r), items);
    }

    private LowStockReportSummary summary(LowStockReport r) {
        List<LowStockReportItem> items = r.getItems();
        return new LowStockReportSummary(r.getId(), r.getReportCode(), r.getWarehouse().getId(), r.getWarehouse().getName(),
                r.getCreatedBy().getName(), r.getStatus().name(), items.size(),
                items.stream().filter(i -> i.getSeverity() == LowStockSeverity.OUT_OF_STOCK).count(),
                items.stream().filter(i -> i.getSeverity() == LowStockSeverity.CRITICAL).count(),
                r.getManagerNote(), r.getBusinessNote(),
                r.getHandledBy() == null ? null : r.getHandledBy().getName(),
                r.getCreatedAt(), r.getAcknowledgedAt(), r.getResolvedAt());
    }

    private String nextCode() {
        String prefix = "LSR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long count = em.createQuery("SELECT COUNT(r) FROM LowStockReport r WHERE r.reportCode LIKE :p", Long.class)
                .setParameter("p", prefix + "%").getSingleResult();
        return prefix + String.format("%03d", count + 1);
    }

    private static int normalizeThreshold(Integer threshold) {
        if (threshold == null) return DEFAULT_THRESHOLD;
        return Math.max(0, Math.min(threshold, 1_000_000));
    }

    private static long safe(Integer value) { return value == null ? 0 : value; }

    private static String trim(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
