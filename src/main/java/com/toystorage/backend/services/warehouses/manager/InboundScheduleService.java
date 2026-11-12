package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.PurchaseOrders;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/**
 * Lich hang ve kho theo ngay (chi doc): don mua hang (PO) cua nha cung cap va hang dieu chuyen den.
 * Ngay giao = ngay da thong nhat > ngay NCC de xuat > ngay du kien tren PO.
 * Khong can biet gio xe den: khi xe toi, Business tao phieu nhap va nhan vien vai tro "Nhan hang" tu nhan viec.
 * Lich nay giup Manager phan bao nhieu nguoi vao vai tro nhan hang cho tung ngay.
 */
@Service
@RequiredArgsConstructor
public class InboundScheduleService {

    private static final Set<String> RECEIVING = Set.of("RECEIVING", "INSPECTED", "CONFIRMED");

    @PersistenceContext
    private EntityManager em;

    private final ManagerOpsAccess access;

    @Transactional(readOnly = true)
    public List<Delivery> schedule(LocalDate from, LocalDate to) {
        Users manager = access.currentWarehouseUser();
        LocalDate today = DailyTaskService.today();
        LocalDate start = from == null ? today : from;
        LocalDate end = to == null ? start.plusDays(6) : to;
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The end date must be after the start date");
        if (ChronoUnit.DAYS.between(start, end) > 62) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The period cannot exceed two months");
        return forWarehouse(manager.getWarehouse().getId(), start, end, today);
    }

    @Transactional(readOnly = true)
    public List<Delivery> forWarehouse(Long w, LocalDate start, LocalDate end, LocalDate today) {
        List<Delivery> result = new ArrayList<>();

        // ---------- Purchase orders from suppliers ----------
        List<PurchaseOrders> orders = em.createQuery("""
                SELECT po FROM PurchaseOrders po LEFT JOIN FETCH po.supplier
                WHERE po.warehouse.id = :w AND po.status IN :st
                """, PurchaseOrders.class)
                .setParameter("w", w)
                .setParameter("st", List.of(PurchaseOrderStatus.ORDERED, PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderStatus.COMPLETED))
                .getResultList();
        if (!orders.isEmpty()) {
            List<Long> ids = orders.stream().map(PurchaseOrders::getId).toList();
            Map<Long, long[]> qty = new HashMap<>();
            for (Object[] row : em.createQuery("""
                    SELECT i.purchaseOrder.id, COUNT(i), SUM(COALESCE(i.confirmedQuantity, i.orderedQuantity)), SUM(COALESCE(i.receivedQuantity, 0))
                    FROM PurchaseOrderItems i WHERE i.purchaseOrder.id IN :ids GROUP BY i.purchaseOrder.id
                    """, Object[].class).setParameter("ids", ids).getResultList()) {
                qty.put((Long) row[0], new long[]{num(row[1]), num(row[2]), num(row[3])});
            }
            Map<Long, GoodsReceipts> latestReceipt = new HashMap<>();
            for (GoodsReceipts g : em.createQuery("""
                    SELECT g FROM GoodsReceipts g LEFT JOIN FETCH g.receivedBy
                    WHERE g.purchaseOrder.id IN :ids ORDER BY g.createdAt DESC
                    """, GoodsReceipts.class).setParameter("ids", ids).getResultList()) {
                latestReceipt.putIfAbsent(g.getPurchaseOrder().getId(), g);
            }
            Map<Long, String> claimStaff = activeClaims(WarehouseTaskType.GOODS_RECEIVING,
                    latestReceipt.values().stream().map(GoodsReceipts::getId).toList());

            for (PurchaseOrders po : orders) {
                String source;
                LocalDate date;
                if (po.getAgreedDeliveryDate() != null) { date = po.getAgreedDeliveryDate(); source = "AGREED"; }
                else if (po.getSupplierProposedDeliveryDate() != null) { date = po.getSupplierProposedDeliveryDate(); source = "SUPPLIER_PROPOSED"; }
                else { date = po.getExpectedDeliveryDate(); source = "EXPECTED"; }
                GoodsReceipts gr = latestReceipt.get(po.getId());
                String status = deliveryStatus(po, gr, date, today);
                boolean inRange = date != null && !date.isBefore(start) && !date.isAfter(end);
                // Hang tre (qua ngay ma chua ve) van hien o hom nay de khong bi bo sot.
                boolean lateCarry = "LATE".equals(status) && !start.isAfter(today) && !end.isBefore(today);
                if (!inRange && !lateCarry) continue;
                long[] q = qty.getOrDefault(po.getId(), new long[]{0, 0, 0});
                String staff = gr == null ? null
                        : claimStaff.getOrDefault(gr.getId(), gr.getReceivedBy() == null ? null : gr.getReceivedBy().getName());
                result.add(new Delivery("SUPPLIER", po.getId(), null, po.getOrderCode(),
                        po.getSupplier() == null ? "—" : po.getSupplier().getName(),
                        po.getSupplier() == null ? null : po.getSupplier().getPhone(),
                        date, source, po.getNote(), status, (int) q[0], q[1], Math.max(0, q[1] - q[2]),
                        gr == null ? null : gr.getReceiptCode(), gr == null ? null : gr.getStatus().name(), staff));
            }
        }

        // ---------- Stock transfers coming to this warehouse ----------
        for (StockTransfer t : em.createQuery("""
                SELECT t FROM StockTransfer t LEFT JOIN FETCH t.fromWarehouse
                WHERE t.toWarehouse.id = :w AND t.status IN :st
                """, StockTransfer.class)
                .setParameter("w", w)
                .setParameter("st", List.of(TransferStatus.SHIPPED, TransferStatus.RECEIVING))
                .getResultList()) {
            LocalDate date = t.getExpectedReceiptDate() != null ? t.getExpectedReceiptDate()
                    : t.getShippedAt() != null ? t.getShippedAt().toLocalDate() : today;
            String status = t.getStatus() == TransferStatus.RECEIVING ? "RECEIVING" : date.isBefore(today) ? "LATE" : "IN_TRANSIT";
            boolean inRange = !date.isBefore(start) && !date.isAfter(end);
            boolean lateCarry = "LATE".equals(status) && !start.isAfter(today) && !end.isBefore(today);
            if (!inRange && !lateCarry) continue;
            long qtyTotal = t.getItems().stream().mapToLong(i -> i.getShippedQuantity() != null && i.getShippedQuantity() > 0
                    ? i.getShippedQuantity() : (i.getApprovedQuantity() == null ? 0 : i.getApprovedQuantity())).sum();
            result.add(new Delivery("TRANSFER", null, t.getId(), t.getTransferCode(),
                    t.getFromWarehouse() == null ? "—" : t.getFromWarehouse().getName(),
                    t.getFromWarehouse() == null ? null : t.getFromWarehouse().getPhone(),
                    date, "EXPECTED_RECEIPT", t.getNotes(), status, t.getItems().size(), qtyTotal, qtyTotal,
                    null, t.getStatus().name(), null));
        }

        result.sort(Comparator.comparing(Delivery::deliveryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Delivery::orderCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * RECEIVED: da nhan xong; RECEIVING: dang kiem; ARRIVED: xe da toi (Business da tao phieu nhap);
     * LATE: qua ngay ma chua toi; EXPECTED: du kien.
     */
    private static String deliveryStatus(PurchaseOrders po, GoodsReceipts gr, LocalDate date, LocalDate today) {
        if (po.getStatus() == PurchaseOrderStatus.COMPLETED) return "RECEIVED";
        if (gr != null && "COMPLETED".equals(gr.getStatus().name()) && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) return "RECEIVED";
        if (gr != null && RECEIVING.contains(gr.getStatus().name())) return "RECEIVING";
        if (gr != null && "CREATED".equals(gr.getStatus().name())) return "ARRIVED";
        if (date != null && date.isBefore(today)) return "LATE";
        return "EXPECTED";
    }

    /** Nhan vien dang giu (claim) cong viec, theo id tham chieu. */
    Map<Long, String> activeClaims(WarehouseTaskType type, Collection<Long> referenceIds) {
        Map<Long, String> map = new HashMap<>();
        if (referenceIds == null || referenceIds.isEmpty()) return map;
        for (WarehouseTaskClaim c : em.createQuery("""
                SELECT c FROM WarehouseTaskClaim c JOIN FETCH c.claimedBy
                WHERE c.taskType = :t AND c.referenceId IN :ids AND c.releasedAt IS NULL
                ORDER BY c.claimedAt DESC
                """, WarehouseTaskClaim.class).setParameter("t", type).setParameter("ids", referenceIds).getResultList()) {
            map.putIfAbsent(c.getReferenceId(), c.getClaimedBy().getName());
        }
        return map;
    }

    private static long num(Object o) { return o == null ? 0 : ((Number) o).longValue(); }
}
