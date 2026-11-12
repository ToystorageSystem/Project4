package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseDailyTask;
import com.toystorage.backend.enums.warehouses.DailyTaskPriority;
import com.toystorage.backend.enums.warehouses.DailyTaskStatus;
import com.toystorage.backend.enums.warehouses.DailyTaskType;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.manager.WarehouseDailyTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/** Phan cong cong viec trong ngay cho nhan vien kho va theo doi tien do. */
@Service
@RequiredArgsConstructor
public class DailyTaskService {

    private static final Set<DailyTaskStatus> OPEN = EnumSet.of(DailyTaskStatus.ASSIGNED, DailyTaskStatus.IN_PROGRESS);

    private final WarehouseDailyTaskRepository tasks;
    private final UserRepository users;
    private final ManagerOpsAccess access;
    private final WarehouseOperationsOverviewService overview;

    // ======================= MANAGER =======================

    @Transactional(readOnly = true)
    public List<StaffOption> staff() {
        Users manager = access.currentWarehouseUser();
        return access.staffOfWarehouse(manager.getWarehouse().getId()).stream()
                .map(u -> new StaffOption(u.getId(), u.getUserCode(), u.getName(), u.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DayBoard board(LocalDate date) {
        Users manager = access.currentWarehouseUser();
        LocalDate day = date == null ? today() : date;
        Long w = manager.getWarehouse().getId();
        List<WarehouseDailyTask> all = tasks.findForWarehouseDay(w, day);
        Map<Long, List<WarehouseDailyTask>> byStaff = new LinkedHashMap<>();
        for (Users s : access.staffOfWarehouse(w)) byStaff.put(s.getId(), new ArrayList<>());
        Map<Long, Users> staffById = new HashMap<>();
        for (WarehouseDailyTask t : all) {
            byStaff.computeIfAbsent(t.getAssignedTo().getId(), k -> new ArrayList<>()).add(t);
            staffById.put(t.getAssignedTo().getId(), t.getAssignedTo());
        }
        Map<Long, Users> known = new HashMap<>();
        access.staffOfWarehouse(w).forEach(u -> known.put(u.getId(), u));
        known.putAll(staffById);

        List<StaffDay> staffDays = new ArrayList<>();
        for (Map.Entry<Long, List<WarehouseDailyTask>> e : byStaff.entrySet()) {
            Users u = known.get(e.getKey());
            List<Task> views = sorted(e.getValue()).stream().map(this::view).toList();
            staffDays.add(new StaffDay(u.getId(), u.getName(), u.getEmail(), views.size(),
                    views.stream().filter(v -> "COMPLETED".equals(v.status())).count(),
                    views.stream().filter(v -> "IN_PROGRESS".equals(v.status())).count(),
                    views.stream().filter(Task::overdue).count(), views));
        }
        List<Task> allViews = all.stream().map(this::view).toList();
        return new DayBoard(day, allViews.size(),
                allViews.stream().filter(v -> "COMPLETED".equals(v.status())).count(),
                allViews.stream().filter(v -> "IN_PROGRESS".equals(v.status())).count(),
                allViews.stream().filter(v -> "ASSIGNED".equals(v.status())).count(),
                allViews.stream().filter(Task::overdue).count(),
                staffDays, suggestions(w, day));
    }

    @Transactional
    public Task create(TaskRequest request) {
        Users manager = access.currentWarehouseUser();
        return view(tasks.save(build(manager, request, request == null ? null : request.staffId())));
    }

    @Transactional
    public List<Task> createBulk(BulkTaskRequest request) {
        if (request == null || request.staffIds() == null || request.staffIds().isEmpty() || request.task() == null) {
            throw bad("Select at least one staff member");
        }
        Users manager = access.currentWarehouseUser();
        List<Task> created = new ArrayList<>();
        for (Long staffId : new LinkedHashSet<>(request.staffIds())) {
            created.add(view(tasks.save(build(manager, request.task(), staffId))));
        }
        return created;
    }

    @Transactional
    public Task update(Long id, TaskRequest request) {
        Users manager = access.currentWarehouseUser();
        WarehouseDailyTask t = ownedByWarehouse(id, manager);
        if (!OPEN.contains(t.getStatus())) throw conflict("Completed or cancelled tasks cannot be edited");
        WarehouseDailyTask changes = build(manager, request, request.staffId() == null ? t.getAssignedTo().getId() : request.staffId());
        t.setAssignedTo(changes.getAssignedTo());
        t.setTaskDate(changes.getTaskDate());
        t.setTaskType(changes.getTaskType());
        t.setTitle(changes.getTitle());
        t.setDescription(changes.getDescription());
        t.setPriority(changes.getPriority());
        t.setDueTime(changes.getDueTime());
        t.setReferenceCode(changes.getReferenceCode());
        t.setReferencePath(changes.getReferencePath());
        t.setTargetQuantity(changes.getTargetQuantity());
        t.setManagerNote(changes.getManagerNote());
        return view(tasks.save(t));
    }

    @Transactional
    public Task cancel(Long id, CancelRequest request) {
        Users manager = access.currentWarehouseUser();
        WarehouseDailyTask t = ownedByWarehouse(id, manager);
        if (!OPEN.contains(t.getStatus())) throw conflict("Only open tasks can be cancelled");
        t.setStatus(DailyTaskStatus.CANCELLED);
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            t.setManagerNote(limit("Cancelled: " + request.reason().trim(), 1000));
        }
        return view(tasks.save(t));
    }

    /** Manager xac nhan hoan thanh thay nhan vien (vd viec lam ngoai he thong). */
    @Transactional
    public Task managerComplete(Long id, StaffProgressRequest request) {
        Users manager = access.currentWarehouseUser();
        WarehouseDailyTask t = ownedByWarehouse(id, manager);
        if (!OPEN.contains(t.getStatus())) throw conflict("This task is already closed");
        complete(t, request, true);
        return view(tasks.save(t));
    }

    /** Sao chep cong viec chua huy tu mot ngay sang ngay khac (vd ca lam viec lap lai). */
    @Transactional
    public List<Task> copy(CopyTasksRequest request) {
        if (request == null || request.fromDate() == null || request.toDate() == null) throw bad("Select both dates");
        if (request.fromDate().equals(request.toDate())) throw bad("Source and target dates must differ");
        if (request.toDate().isBefore(today())) throw bad("Tasks cannot be copied into the past");
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        List<Task> result = new ArrayList<>();
        for (WarehouseDailyTask source : tasks.findForWarehouseDay(w, request.fromDate())) {
            if (source.getStatus() == DailyTaskStatus.CANCELLED) continue;
            WarehouseDailyTask copy = WarehouseDailyTask.builder()
                    .taskCode(nextCode(request.toDate()))
                    .warehouse(manager.getWarehouse()).assignedTo(source.getAssignedTo()).assignedBy(manager)
                    .taskDate(request.toDate()).taskType(source.getTaskType()).title(source.getTitle())
                    .description(source.getDescription()).priority(source.getPriority()).dueTime(source.getDueTime())
                    .referenceCode(source.getReferenceCode()).referencePath(source.getReferencePath())
                    .targetQuantity(source.getTargetQuantity()).managerNote(source.getManagerNote())
                    .status(DailyTaskStatus.ASSIGNED).build();
            result.add(view(tasks.save(copy)));
        }
        if (result.isEmpty()) throw bad("There are no tasks to copy on " + request.fromDate());
        return result;
    }

    // ======================= STAFF =======================

    @Transactional(readOnly = true)
    public List<Task> myTasks(LocalDate date) {
        Users staff = access.currentWarehouseUser();
        return sorted(tasks.findForStaffDay(staff.getId(), date == null ? today() : date)).stream().map(this::view).toList();
    }

    @Transactional
    public Task start(Long id) {
        WarehouseDailyTask t = ownedByStaff(id);
        if (t.getStatus() != DailyTaskStatus.ASSIGNED) throw conflict("Only assigned tasks can be started");
        t.setStatus(DailyTaskStatus.IN_PROGRESS);
        t.setStartedAt(LocalDateTime.now(WarehouseOperationsOverviewService.ZONE));
        return view(tasks.save(t));
    }

    @Transactional
    public Task staffComplete(Long id, StaffProgressRequest request) {
        WarehouseDailyTask t = ownedByStaff(id);
        if (!OPEN.contains(t.getStatus())) throw conflict("This task is already closed");
        complete(t, request, false);
        return view(tasks.save(t));
    }

    // ======================= INTERNAL =======================

    private void complete(WarehouseDailyTask t, StaffProgressRequest request, boolean byManager) {
        LocalDateTime now = LocalDateTime.now(WarehouseOperationsOverviewService.ZONE);
        if (t.getStartedAt() == null) t.setStartedAt(now);
        t.setCompletedAt(now);
        t.setStatus(DailyTaskStatus.COMPLETED);
        if (request != null) {
            if (request.completedQuantity() != null) {
                if (request.completedQuantity() < 0) throw bad("Completed quantity cannot be negative");
                t.setCompletedQuantity(request.completedQuantity());
            }
            if (request.note() != null && !request.note().isBlank()) {
                if (byManager) t.setManagerNote(limit(request.note().trim(), 1000));
                else t.setStaffNote(limit(request.note().trim(), 1000));
            }
        }
        if (t.getCompletedQuantity() == null && t.getTargetQuantity() != null) t.setCompletedQuantity(t.getTargetQuantity());
    }

    private WarehouseDailyTask build(Users manager, TaskRequest r, Long staffId) {
        if (r == null) throw bad("Task details are required");
        if (staffId == null) throw bad("Select a staff member");
        Users staff = users.findById(staffId).orElseThrow(() -> bad("Staff member not found"));
        boolean sameWarehouse = access.staffOfWarehouse(manager.getWarehouse().getId()).stream()
                .anyMatch(s -> s.getId().equals(staff.getId()));
        if (!sameWarehouse) throw bad("You can only assign tasks to active staff of your warehouse");
        LocalDate date = r.taskDate() == null ? today() : r.taskDate();
        if (date.isBefore(today())) throw bad("Tasks cannot be assigned in the past");
        String title = r.title() == null ? "" : r.title().trim();
        if (title.length() < 3) throw bad("Title must contain at least 3 characters");
        if (r.targetQuantity() != null && r.targetQuantity() < 0) throw bad("Target quantity cannot be negative");
        String path = r.referencePath() == null ? null : r.referencePath().trim();
        if (path != null && !path.isEmpty() && !path.startsWith("/warehouse/")) throw bad("Reference page must be a warehouse page");
        return WarehouseDailyTask.builder()
                .taskCode(nextCode(date))
                .warehouse(manager.getWarehouse()).assignedTo(staff).assignedBy(manager)
                .taskDate(date)
                .taskType(parse(DailyTaskType.class, r.taskType(), DailyTaskType.OTHER, "task type"))
                .title(limit(title, 200))
                .description(limit(r.description(), 1000))
                .priority(parse(DailyTaskPriority.class, r.priority(), DailyTaskPriority.NORMAL, "priority"))
                .dueTime(r.dueTime())
                .referenceCode(limit(r.referenceCode(), 100))
                .referencePath(path == null || path.isEmpty() ? defaultPath(r.taskType()) : limit(path, 200))
                .targetQuantity(r.targetQuantity())
                .managerNote(limit(r.managerNote(), 1000))
                .status(DailyTaskStatus.ASSIGNED)
                .build();
    }

    private static String defaultPath(String type) {
        if (type == null) return null;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "RECEIVING" -> "/warehouse/receiving/process";
            case "PUTAWAY", "ARRANGEMENT" -> "/warehouse/staff-putaway";
            case "PICKING", "TRANSFER" -> "/warehouse/picking";
            case "PACKING" -> "/warehouse/staff-packing";
            case "STOCK_COUNT" -> "/warehouse/staff-counts";
            case "DAMAGE_HANDLING", "SHORTAGE_HANDLING" -> "/warehouse/staff-damage";
            case "STORE_RETURN" -> "/warehouse/store-returns";
            default -> null;
        };
    }

    /** Goi y viec can lam hom nay dua tren du lieu nhap / xuat / kiem ke / hang hong dang mo. */
    private List<Suggestion> suggestions(Long w, LocalDate day) {
        List<Suggestion> list = new ArrayList<>();
        Map<String, Long> receipts = overview.countByStatus(
                "SELECT g.status, COUNT(g) FROM GoodsReceipts g WHERE g.warehouse.id = :w GROUP BY g.status", w);
        Map<String, Long> putaway = overview.countByStatus(
                "SELECT p.status, COUNT(p) FROM PutawayTasks p WHERE p.warehouse.id = :w GROUP BY p.status", w);
        Map<String, Long> outbound = overview.countByStatus(
                "SELECT t.status, COUNT(t) FROM StockTransfer t WHERE t.fromWarehouse.id = :w GROUP BY t.status", w);
        Map<String, Long> inbound = overview.countByStatus(
                "SELECT t.status, COUNT(t) FROM StockTransfer t WHERE t.toWarehouse.id = :w GROUP BY t.status", w);
        Map<String, Long> damaged = overview.countByStatus(
                "SELECT d.status, COUNT(d) FROM DamagedGoodsReports d WHERE d.warehouse.id = :w GROUP BY d.status", w);
        Map<String, Long> discrepancies = overview.countByStatus(
                "SELECT r.status, COUNT(r) FROM DiscrepancyReports r WHERE r.warehouse.id = :w GROUP BY r.status", w);

        add(list, "RECEIVING", "Receive and inspect supplier deliveries", "Goods receipts waiting to be received or inspected",
                "/warehouse/receiving/process", "HIGH", sum(receipts, "CREATED", "RECEIVING"));
        add(list, "PUTAWAY", "Put received goods away", "Putaway tasks that are still open",
                "/warehouse/staff-putaway", "HIGH", sum(putaway, "PENDING", "AVAILABLE", "IN_PROGRESS"));
        add(list, "PICKING", "Pick outbound transfers", "Transfers waiting to be picked",
                "/warehouse/picking", "HIGH", sum(outbound, "CREATED", "PICKING"));
        add(list, "PACKING", "Pack and seal picked transfers", "Transfers in packing",
                "/warehouse/staff-packing", "NORMAL", sum(outbound, "PACKING"));
        add(list, "DISPATCH", "Hand packed shipments over to drivers", "Packed transfers waiting for dispatch",
                "/warehouse/dispatch", "NORMAL", sum(outbound, "PACKED"));
        add(list, "TRANSFER", "Receive incoming transfers", "Transfers shipped to this warehouse",
                "/warehouse/receiving/process", "NORMAL", sum(inbound, "SHIPPED", "RECEIVING"));
        add(list, "DAMAGE_HANDLING", "Inspect and isolate damaged goods", "Damage reports not yet resolved",
                "/warehouse/staff-damage", "HIGH", sum(damaged, "REPORTED", "INSPECTING", "APPROVED"));
        add(list, "SHORTAGE_HANDLING", "Investigate shortages and discrepancies", "Open discrepancy reports",
                "/warehouse/staff-damage", "NORMAL", sum(discrepancies, "OPEN", "INVESTIGATING"));
        overview.countWindows(w, day).stream().filter(CountWindow::active).findFirst().ifPresent(window ->
                list.add(new Suggestion("STOCK_COUNT",
                        ("MID_MONTH".equals(window.type()) ? "Mid-month" : "End-of-month") + " stock count week",
                        "Stock counts are allowed " + window.start() + " – " + window.end() + ". Assign count zones to staff.",
                        "/warehouse/staff-counts", "URGENT", Math.max(1, window.plans().size()))));
        list.add(new Suggestion("ARRANGEMENT", "Arrange shelves and storage locations",
                "Consolidate partial pallets, keep fast movers near dispatch, clear the receiving area.",
                "/warehouse/staff-putaway", "LOW", 0));
        return list;
    }

    private static void add(List<Suggestion> list, String type, String title, String description, String path,
                            String priority, long count) {
        if (count > 0) list.add(new Suggestion(type, title, description + ": " + count, path, priority, count));
    }

    private static long sum(Map<String, Long> map, String... keys) {
        long total = 0;
        for (String k : keys) total += map.getOrDefault(k, 0L);
        return total;
    }

    private List<WarehouseDailyTask> sorted(List<WarehouseDailyTask> list) {
        List<WarehouseDailyTask> copy = new ArrayList<>(list);
        copy.sort(Comparator
                .comparing((WarehouseDailyTask t) -> t.getStatus() == DailyTaskStatus.COMPLETED || t.getStatus() == DailyTaskStatus.CANCELLED)
                .thenComparing(t -> -t.getPriority().ordinal())
                .thenComparing(WarehouseDailyTask::getDueTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WarehouseDailyTask::getId));
        return copy;
    }

    Task view(WarehouseDailyTask t) {
        boolean overdue = OPEN.contains(t.getStatus()) && isPastDeadline(t);
        return new Task(t.getId(), t.getTaskCode(), t.getTaskDate(), t.getTaskType().name(), t.getTitle(),
                t.getDescription(), t.getPriority().name(), t.getStatus().name(), t.getDueTime(),
                t.getReferenceCode(), t.getReferencePath(), t.getTargetQuantity(), t.getCompletedQuantity(),
                t.getAssignedTo().getId(), t.getAssignedTo().getName(), t.getAssignedBy().getName(),
                t.getStaffNote(), t.getManagerNote(), t.getStartedAt(), t.getCompletedAt(), overdue, t.isOnTime());
    }

    static boolean isPastDeadline(WarehouseDailyTask t) {
        LocalDateTime deadline = t.getTaskDate().atTime(t.getDueTime() == null ? LocalTime.of(23, 59, 59) : t.getDueTime());
        return LocalDateTime.now(WarehouseOperationsOverviewService.ZONE).isAfter(deadline);
    }

    private WarehouseDailyTask ownedByWarehouse(Long id, Users manager) {
        WarehouseDailyTask t = tasks.findDetail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!t.getWarehouse().getId().equals(manager.getWarehouse().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return t;
    }

    private WarehouseDailyTask ownedByStaff(Long id) {
        Users staff = access.currentWarehouseUser();
        WarehouseDailyTask t = tasks.findDetail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!t.getAssignedTo().getId().equals(staff.getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        return t;
    }

    private String nextCode(LocalDate date) {
        return "WT-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + Long.toString(System.nanoTime() % 1_000_000L, 36).toUpperCase(Locale.ROOT)
                + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback, String label) {
        if (value == null || value.isBlank()) return fallback;
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw bad("Unknown " + label + ": " + value); }
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        return v.length() > max ? v.substring(0, max) : v;
    }

    static LocalDate today() { return LocalDate.now(WarehouseOperationsOverviewService.ZONE); }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }

    private static ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }
}
