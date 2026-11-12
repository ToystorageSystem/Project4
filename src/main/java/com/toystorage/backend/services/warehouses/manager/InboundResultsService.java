package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.entity.inventories.DamagedStockLot;
import com.toystorage.backend.entity.inventories.DiscrepancyItems;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceivingIncidentReports;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.enums.stores.StoreReturnStatus;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.repository.inventories.damagedlot.DamagedStockLotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * "Review receiving" cua Warehouse Manager theo nghiep vu moi:
 *  - Phieu nhap NCC va hang tra tu cua hang deu hoan thanh ngay khi nhan vien kiem xong.
 *  - Manager xem ket qua (dat / hong / thieu / du), lo hang hong, bien ban su co.
 *  - Moi chenh lech (thieu / hong / du, ke ca tu kiem ke) Manager chon: luu noi bo hoac gui Business.
 */
@Service
@RequiredArgsConstructor
public class InboundResultsService {

    public record Line(Long productId, String productCode, String productName, int expected, int received,
                       int passed, int damaged, int shortage, int surplus, String condition, String note) {}

    public record Issue(Long id, String code, String source, Long referenceId, String referenceCode, Long productId,
                        String productName, String type, String severity, String status, String handledBy,
                        String decision, String description, String resolutionNote, LocalDateTime createdAt) {}

    public record Result(String type, Long id, String code, String from, String inspectedBy,
                         LocalDateTime completedAt, int lines, long passed, long damaged, long shortage, long surplus,
                         List<String> lotCodes, Long incidentReportId, List<Line> items, List<Issue> issues) {}

    public record Lot(Long id, String lotCode, String sourceType, String sourceCode, Long productId,
                      String productCode, String productName, int quantity, String location, String note,
                      String recordedBy, LocalDateTime createdAt) {}

    public record DecisionRequest(String decision, String note) {}

    @PersistenceContext
    private EntityManager em;

    private final ManagerOpsAccess access;
    private final DamagedStockLotRepository lots;

    // ===================== RESULTS =====================

    @Transactional(readOnly = true)
    public List<Result> results(String type, Integer days) {
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        LocalDateTime since = LocalDateTime.now().minusDays(days == null || days <= 0 ? 30 : Math.min(days, 366));
        return "STORE_RETURN".equalsIgnoreCase(type) ? storeReturns(w, since) : supplierReceipts(w, since);
    }

    private List<Result> supplierReceipts(Long w, LocalDateTime since) {
        List<GoodsReceipts> receipts = em.createQuery("""
                SELECT g FROM GoodsReceipts g LEFT JOIN FETCH g.purchaseOrder po LEFT JOIN FETCH po.supplier
                WHERE g.warehouse.id = :w AND g.status = :s AND g.updatedAt >= :since
                ORDER BY g.updatedAt DESC
                """, GoodsReceipts.class)
                .setParameter("w", w).setParameter("s", GoodsReceiptStatus.COMPLETED).setParameter("since", since)
                .setMaxResults(60).getResultList();
        List<Result> out = new ArrayList<>();
        for (GoodsReceipts g : receipts) {
            List<GoodsReceiptItems> items = em.createQuery(
                    "SELECT i FROM GoodsReceiptItems i JOIN FETCH i.product WHERE i.goodsReceipt.id = :id", GoodsReceiptItems.class)
                    .setParameter("id", g.getId()).getResultList();
            List<Line> lines = items.stream().map(i -> new Line(i.getProduct().getId(), i.getProduct().getProductsCode(),
                    i.getProduct().getName(), n(i.getExpectedQuantity()), n(i.getActualQuantity()), n(i.getAcceptedQuantity()),
                    n(i.getDamagedQuantity()), n(i.getShortageQuantity()), n(i.getSurplusQuantity()), null, null)).toList();
            WarehouseTaskClaim claim = em.createQuery("""
                    SELECT c FROM WarehouseTaskClaim c JOIN FETCH c.claimedBy
                    WHERE c.taskType = :t AND c.referenceId = :id ORDER BY c.claimedAt DESC
                    """, WarehouseTaskClaim.class).setParameter("t", WarehouseTaskType.GOODS_RECEIVING)
                    .setParameter("id", g.getId()).setMaxResults(1).getResultStream().findFirst().orElse(null);
            Long incidentId = em.createQuery(
                    "SELECT r FROM ReceivingIncidentReports r WHERE r.goodsReceipt.id = :id ORDER BY r.createdAt DESC",
                    ReceivingIncidentReports.class).setParameter("id", g.getId()).setMaxResults(1)
                    .getResultStream().findFirst().map(ReceivingIncidentReports::getId).orElse(null);
            String from = g.getPurchaseOrder() == null || g.getPurchaseOrder().getSupplier() == null
                    ? "Supplier" : g.getPurchaseOrder().getSupplier().getName();
            out.add(result("SUPPLIER", g.getId(), g.getReceiptCode(), from,
                    claim == null ? null : claim.getClaimedBy().getName(),
                    g.getInspectionConfirmedAt() != null ? g.getInspectionConfirmedAt() : g.getUpdatedAt(),
                    lines, "GOODS_RECEIPT", incidentId, DiscrepancyReferenceType.GOODS_RECEIPT));
        }
        return out;
    }

    private List<Result> storeReturns(Long w, LocalDateTime since) {
        List<StoreReturns> returns = em.createQuery("""
                SELECT r FROM StoreReturns r LEFT JOIN FETCH r.store LEFT JOIN FETCH r.inspectedBy
                WHERE r.warehouse.id = :w AND r.status = :s AND r.updatedAt >= :since
                ORDER BY r.updatedAt DESC
                """, StoreReturns.class)
                .setParameter("w", w).setParameter("s", StoreReturnStatus.RECEIVED).setParameter("since", since)
                .setMaxResults(60).getResultList();
        List<Result> out = new ArrayList<>();
        for (StoreReturns r : returns) {
            List<StoreReturnItems> items = em.createQuery(
                    "SELECT i FROM StoreReturnItems i JOIN FETCH i.product WHERE i.storeReturn.id = :id", StoreReturnItems.class)
                    .setParameter("id", r.getId()).getResultList();
            List<Line> lines = items.stream().map(i -> {
                int expected = i.getIssuedQuantity() != null && i.getIssuedQuantity() > 0 ? i.getIssuedQuantity()
                        : i.getApprovedQuantity() != null && i.getApprovedQuantity() > 0 ? i.getApprovedQuantity() : n(i.getRequestedQuantity());
                int received = n(i.getReceivedQuantity());
                boolean normal = i.getConditionStatus() == null || "NORMAL".equals(i.getConditionStatus().name());
                return new Line(i.getProduct().getId(), i.getProduct().getProductsCode(), i.getProduct().getName(),
                        expected, received, normal ? received : 0, normal ? 0 : received,
                        Math.max(0, expected - received), Math.max(0, received - expected),
                        i.getConditionStatus() == null ? null : i.getConditionStatus().name(), i.getNote());
            }).toList();
            out.add(result("STORE_RETURN", r.getId(), r.getReturnCode(), r.getStore() == null ? "Store" : r.getStore().getName(),
                    r.getInspectedBy() == null ? null : r.getInspectedBy().getName(),
                    r.getReceivedAt() != null ? r.getReceivedAt() : r.getUpdatedAt(), lines, "STORE_RETURN", null,
                    DiscrepancyReferenceType.STORE_RETURN));
        }
        return out;
    }

    private Result result(String type, Long id, String code, String from, String inspectedBy, LocalDateTime completedAt,
                          List<Line> lines, String lotSource, Long incidentId, DiscrepancyReferenceType refType) {
        List<String> lotCodes = lots.findForSource(lotSource, id).stream().map(DamagedStockLot::getLotCode).distinct().toList();
        return new Result(type, id, code, from, inspectedBy, completedAt, lines.size(),
                lines.stream().mapToLong(Line::passed).sum(), lines.stream().mapToLong(Line::damaged).sum(),
                lines.stream().mapToLong(Line::shortage).sum(), lines.stream().mapToLong(Line::surplus).sum(),
                lotCodes, incidentId, lines, issuesFor(refType, id, code));
    }

    /** Chenh lech tu kiem ke (hang hong / thieu / du ghi nhan khi kiem ton thuc te). */
    @Transactional(readOnly = true)
    public List<Issue> stockCountIssues(Integer days) {
        Users manager = access.currentWarehouseUser();
        LocalDateTime since = LocalDateTime.now().minusDays(days == null || days <= 0 ? 60 : Math.min(days, 366));
        List<DiscrepancyReports> reports = em.createQuery("""
                SELECT r FROM DiscrepancyReports r
                WHERE r.warehouse.id = :w AND r.referenceType = :t AND r.createdAt >= :since
                ORDER BY r.createdAt DESC
                """, DiscrepancyReports.class)
                .setParameter("w", manager.getWarehouse().getId()).setParameter("t", DiscrepancyReferenceType.STOCK_COUNT)
                .setParameter("since", since).getResultList();
        Map<Long, String> countCodes = new HashMap<>();
        return reports.stream().map(r -> issue(r, countCodes.computeIfAbsent(r.getReferenceId(), id ->
                em.createQuery("SELECT c.countCode FROM StockCounts c WHERE c.id = :id", String.class)
                        .setParameter("id", id).getResultStream().findFirst().orElse("SC-" + id)))).toList();
    }

    @Transactional(readOnly = true)
    public List<Lot> damagedLots() {
        Users manager = access.currentWarehouseUser();
        return lots.findForWarehouse(manager.getWarehouse().getId()).stream().map(l -> new Lot(l.getId(), l.getLotCode(),
                l.getSourceType(), l.getSourceCode(), l.getProduct().getId(), l.getProduct().getProductsCode(),
                l.getProduct().getName(), l.getQuantity(), l.getLocation().getWarehouseCode(), l.getNote(),
                l.getRecordedBy() == null ? null : l.getRecordedBy().getName(), l.getCreatedAt())).toList();
    }

    // ===================== DECISION =====================

    /**
     * INTERNAL: luu noi bo (kho tu chiu / da dieu chinh ton) -> RESOLVED.
     * BUSINESS: gui Business giai quyet (lam viec voi nha cung cap / cua hang) -> INVESTIGATING, BUSINESS_MANAGER.
     */
    @Transactional
    public Issue decide(Long discrepancyId, DecisionRequest request) {
        Users manager = access.currentWarehouseUser();
        DiscrepancyReports r = em.find(DiscrepancyReports.class, discrepancyId);
        if (r == null || r.getWarehouse() == null || !r.getWarehouse().getId().equals(manager.getWarehouse().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discrepancy not found");
        }
        if (r.getStatus() == DiscrepancyStatus.RESOLVED || r.getStatus() == DiscrepancyStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This discrepancy has already been closed");
        }
        if ("BUSINESS_MANAGER".equals(r.getResponsibleParty()) && r.getStatus() == DiscrepancyStatus.INVESTIGATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This discrepancy has already been sent to Business");
        }
        String decision = request == null || request.decision() == null ? "" : request.decision().trim().toUpperCase(Locale.ROOT);
        String note = request == null || request.note() == null ? "" : request.note().trim();
        LocalDateTime now = LocalDateTime.now();
        r.setReviewedBy(manager);
        r.setReviewedAt(now);
        switch (decision) {
            case "INTERNAL" -> {
                r.setStatus(DiscrepancyStatus.RESOLVED);
                r.setResponsibleParty("WAREHOUSE_INTERNAL");
                r.setResolutionNote(note.isEmpty() ? "Recorded internally by the warehouse" : limit(note));
                r.setResolvedBy(manager);
                r.setResolvedAt(now);
            }
            case "BUSINESS" -> {
                if (note.length() < 3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tell Business what needs to be done (at least 3 characters)");
                r.setStatus(DiscrepancyStatus.INVESTIGATING);
                r.setResponsibleParty("BUSINESS_MANAGER");
                r.setResolutionNote(limit(note));
                ensureBusinessItem(r);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be INTERNAL or BUSINESS");
        }
        em.merge(r);
        return issue(r, null);
    }

    /** Business can it nhat mot dong hang de xu ly; tao dong neu bao cao chua co. */
    private void ensureBusinessItem(DiscrepancyReports r) {
        Long count = em.createQuery("SELECT COUNT(i) FROM DiscrepancyItems i WHERE i.discrepancyReport.id = :id", Long.class)
                .setParameter("id", r.getId()).getSingleResult();
        if (count > 0 || r.getProductId() == null) return;
        int expected = 0, actual = 0;
        if (r.getReferenceType() == DiscrepancyReferenceType.GOODS_RECEIPT) {
            GoodsReceiptItems gi = em.createQuery(
                    "SELECT i FROM GoodsReceiptItems i WHERE i.goodsReceipt.id = :g AND i.product.id = :p", GoodsReceiptItems.class)
                    .setParameter("g", r.getReferenceId()).setParameter("p", r.getProductId())
                    .getResultStream().findFirst().orElse(null);
            if (gi != null) { expected = n(gi.getExpectedQuantity()); actual = n(gi.getAcceptedQuantity()); }
        }
        DiscrepancyItems item = new DiscrepancyItems();
        item.setDiscrepancyReport(r);
        item.setProduct(em.getReference(Products.class, r.getProductId()));
        item.setExpectedQuantity(expected);
        item.setActualQuantity(actual);
        item.setDifferenceQuantity(actual - expected);
        item.setDiscrepancyItemsCode("DI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        em.persist(item);
    }

    // ===================== INTERNAL =====================

    private List<Issue> issuesFor(DiscrepancyReferenceType type, Long id, String code) {
        return em.createQuery("""
                SELECT r FROM DiscrepancyReports r WHERE r.referenceType = :t AND r.referenceId = :id ORDER BY r.createdAt ASC
                """, DiscrepancyReports.class).setParameter("t", type).setParameter("id", id).getResultList()
                .stream().filter(r -> !"SYSTEM".equals(r.getResponsibleParty()))
                .map(r -> issue(r, code)).toList();
    }

    private Issue issue(DiscrepancyReports r, String referenceCode) {
        String productName = r.getProductId() == null ? null : em.createQuery(
                "SELECT p.name FROM Products p WHERE p.id = :id", String.class).setParameter("id", r.getProductId())
                .getResultStream().findFirst().orElse(null);
        String decision = "BUSINESS_MANAGER".equals(r.getResponsibleParty()) ? "BUSINESS"
                : "WAREHOUSE_INTERNAL".equals(r.getResponsibleParty()) ? "INTERNAL"
                : r.getStatus() == DiscrepancyStatus.RESOLVED ? "CLOSED" : "PENDING";
        return new Issue(r.getId(), r.getReportCode(), r.getReferenceType() == null ? null : r.getReferenceType().name(),
                r.getReferenceId(), referenceCode, r.getProductId(), productName,
                r.getDiscrepancyType() == null ? null : r.getDiscrepancyType().name(),
                r.getSeverity() == null ? null : r.getSeverity().name(),
                r.getStatus() == null ? null : r.getStatus().name(),
                r.getReviewedBy() == null ? null : r.getReviewedBy().getName(), decision,
                r.getDescription(), r.getResolutionNote(), r.getCreatedAt());
    }

    private static int n(Integer v) { return v == null ? 0 : v; }

    private static String limit(String v) { return v.length() > 1000 ? v.substring(0, 1000) : v; }
}
