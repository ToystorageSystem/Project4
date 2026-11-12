package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.config.StockCountWindowPolicy;
import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.services.inventories.lowstock.LowStockReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/**
 * Tong quan van hanh cho Warehouse Manager: nhap / xuat / dieu chuyen, hang hong, chenh lech,
 * lich kiem ke giua thang & cuoi thang. Chi doc du lieu, khong thay doi nghiep vu hien co.
 */
@Service
@RequiredArgsConstructor
public class WarehouseOperationsOverviewService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @PersistenceContext
    private EntityManager em;

    private final ManagerOpsAccess access;
    private final LowStockReportService lowStockService;

    @Transactional(readOnly = true)
    public Overview overview() {
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        LocalDate today = LocalDate.now(ZONE);
        long lowStock = lowStockService.preview(null).items().size();
        return new Overview(
                today,
                countByStatus("SELECT g.status, COUNT(g) FROM GoodsReceipts g WHERE g.warehouse.id = :w GROUP BY g.status", w),
                countByStatus("SELECT t.status, COUNT(t) FROM StockTransfer t WHERE t.toWarehouse.id = :w GROUP BY t.status", w),
                countByStatus("SELECT t.status, COUNT(t) FROM StockTransfer t WHERE t.fromWarehouse.id = :w GROUP BY t.status", w),
                countByStatus("SELECT p.status, COUNT(p) FROM PutawayTasks p WHERE p.warehouse.id = :w GROUP BY p.status", w),
                countByStatus("SELECT d.status, COUNT(d) FROM DamagedGoodsReports d WHERE d.warehouse.id = :w GROUP BY d.status", w),
                countByStatus("SELECT r.status, COUNT(r) FROM DiscrepancyReports r WHERE r.warehouse.id = :w GROUP BY r.status", w),
                countByStatus("SELECT c.status, COUNT(c) FROM StockCounts c WHERE c.warehouse.id = :w GROUP BY c.status", w),
                recentMovements(w),
                countWindows(w, today),
                lowStock
        );
    }

    /** Dem theo trang thai; dung chung cho goi y cong viec trong ngay. */
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus(String jpql, Long warehouseId) {
        Map<String, Long> result = new TreeMap<>();
        List<Object[]> rows = em.createQuery(jpql, Object[].class).setParameter("w", warehouseId).getResultList();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    private List<Movement> recentMovements(Long w) {
        List<Movement> list = new ArrayList<>();
        List<StockTransfer> transfers = em.createQuery("""
                SELECT t FROM StockTransfer t
                LEFT JOIN FETCH t.fromWarehouse LEFT JOIN FETCH t.toWarehouse
                WHERE t.fromWarehouse.id = :w OR t.toWarehouse.id = :w
                ORDER BY t.createdAt DESC
                """, StockTransfer.class).setParameter("w", w).setMaxResults(30).getResultList();
        for (StockTransfer t : transfers) {
            boolean outbound = t.getFromWarehouse() != null && w.equals(t.getFromWarehouse().getId());
            String counterpart = outbound
                    ? (t.getToWarehouse() == null ? "—" : t.getToWarehouse().getName())
                    : (t.getFromWarehouse() == null ? "—" : t.getFromWarehouse().getName());
            list.add(new Movement(t.getId(), t.getTransferCode(), outbound ? "OUTBOUND_TRANSFER" : "INBOUND_TRANSFER",
                    counterpart, name(t.getStatus()), name(t.getTransferType()),
                    outbound ? t.getExpectedShipmentDate() : t.getExpectedReceiptDate(), t.getCreatedAt()));
        }
        List<GoodsReceipts> receipts = em.createQuery("""
                SELECT g FROM GoodsReceipts g LEFT JOIN FETCH g.purchaseOrder po LEFT JOIN FETCH po.supplier
                WHERE g.warehouse.id = :w ORDER BY g.createdAt DESC
                """, GoodsReceipts.class).setParameter("w", w).setMaxResults(30).getResultList();
        for (GoodsReceipts g : receipts) {
            String supplier = g.getPurchaseOrder() != null && g.getPurchaseOrder().getSupplier() != null
                    ? g.getPurchaseOrder().getSupplier().getName() : "Supplier";
            LocalDate expected = g.getPurchaseOrder() == null ? null : g.getPurchaseOrder().getExpectedDeliveryDate();
            list.add(new Movement(g.getId(), g.getReceiptCode(), "INBOUND_RECEIPT", supplier,
                    name(g.getStatus()), "PURCHASE", expected, g.getCreatedAt()));
        }
        list.sort(Comparator.comparing(Movement::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return list.size() > 40 ? list.subList(0, 40) : list;
    }

    /** Hai dot kiem ke moi thang (tuan chua ngay 15 va tuan lam viec cuoi thang) cho thang nay va thang sau. */
    @Transactional(readOnly = true)
    public List<CountWindow> countWindows(Long w, LocalDate today) {
        List<CountWindow> result = new ArrayList<>();
        for (int offset = 0; offset < 2; offset++) {
            for (StockCountWindowPolicy.Window window : StockCountWindowPolicy.windows(YearMonth.from(today).plusMonths(offset))) {
                if (window.end().isBefore(today.minusDays(31))) continue;
                List<StockCounts> plans = em.createQuery("""
                        SELECT c FROM StockCounts c LEFT JOIN FETCH c.assignedTo
                        WHERE c.warehouse.id = :w AND c.scheduledDate BETWEEN :a AND :b
                        ORDER BY c.scheduledDate ASC, c.id ASC
                        """, StockCounts.class)
                        .setParameter("w", w).setParameter("a", window.start()).setParameter("b", window.end())
                        .getResultList();
                result.add(new CountWindow(window.type().name(), window.start(), window.end(),
                        window.contains(today), today.isBefore(window.start()),
                        plans.stream().map(c -> new CountPlan(c.getId(), c.getCountCode(), name(c.getCountType()),
                                name(c.getStatus()), c.getScheduledDate(),
                                c.getAssignedTo() == null ? null : c.getAssignedTo().getName())).toList()));
            }
        }
        return result;
    }

    private static String name(Object value) { return value == null ? null : value.toString(); }
}
