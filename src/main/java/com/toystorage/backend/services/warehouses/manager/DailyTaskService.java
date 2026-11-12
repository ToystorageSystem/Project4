package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.StockCountStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.toystorage.backend.entity.warehouses.WarehouseDailyTask;
import com.toystorage.backend.enums.warehouses.DailyTaskPriority;
import com.toystorage.backend.enums.warehouses.DailyTaskStatus;
import com.toystorage.backend.enums.warehouses.DailyTaskType;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.manager.WarehouseDailyTaskRepository;
import com.toystorage.backend.repository.warehouses.manager.StaffShiftAssignmentRepository;
import com.toystorage.backend.entity.warehouses.StaffShiftAssignment;
import com.toystorage.backend.enums.warehouses.WorkRole;
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
    private final InboundScheduleService inbound;
    private final StaffShiftAssignmentRepository shiftRepository;

    @PersistenceContext
    private EntityManager em;

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
                staffDays);
    }

    @Transactional
    public Task create(TaskRequest request) {
        Users manager = access.currentWarehouseUser();
        WarehouseDailyTask saved = tasks.save(build(manager, request, request == null ? null : request.staffId()));
        addFollowUpPutaway(manager, saved, request);
        return view(saved);
    }

    @Transactional
    public List<Task> createBulk(BulkTaskRequest request) {
        if (request == null || request.staffIds() == null || request.staffIds().isEmpty() || request.task() == null) {
            throw bad("Select at least one staff member");
        }
        Users manager = access.currentWarehouseUser();
        List<Task> created = new ArrayList<>();
        for (Long staffId : new LinkedHashSet<>(request.staffIds())) {
            WarehouseDailyTask saved = tasks.save(build(manager, request.task(), staffId));
            created.add(view(saved));
            WarehouseDailyTask putaway = addFollowUpPutaway(manager, saved, request.task());
            if (putaway != null) created.add(view(putaway));
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
        DailyTaskType type = parse(DailyTaskType.class, r.taskType(), DailyTaskType.OTHER, "task type");
        if (type == DailyTaskType.PUTAWAY) requireReceiverForPutaway(manager.getWarehouse().getId(), r.referenceCode(), staff);
        if (type == DailyTaskType.RECEIVING) requireSameReceiver(manager.getWarehouse().getId(), r.referenceCode(), staff);
        String path = r.referencePath() == null ? null : r.referencePath().trim();
        if (path != null && !path.isEmpty() && !path.startsWith("/warehouse/")) throw bad("Reference page must be a warehouse page");
        return WarehouseDailyTask.builder()
                .taskCode(nextCode(date))
                .warehouse(manager.getWarehouse()).assignedTo(staff).assignedBy(manager)
                .taskDate(date)
                .taskType(type)
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

    // ======================= JOBS OF THE DAY =======================

    /** Manager: tat ca dau viec trong ngay de theo doi / giao dich danh. */
    @Transactional(readOnly = true)
    public List<Job> jobs(LocalDate date) {
        Users manager = access.currentWarehouseUser();
        return jobsFor(manager.getWarehouse().getId(), date == null ? today() : date);
    }

    /**
     * Dau viec trong ngay: hang ve (PO / dieu chuyen), cat hang len ke, lay hang, dong goi, ban giao,
     * hang hong, hang thieu, kiem ke (tuan dau thang / giua thang), sap xep ke.
     */
    private List<Job> jobsFor(Long w, LocalDate day) {
        Map<String, List<String>> assigned = new HashMap<>();
        Set<String> taken = new HashSet<>();
        for (WarehouseDailyTask t : tasks.findForWarehouseDay(w, day)) {
            if (t.getStatus() == DailyTaskStatus.CANCELLED || t.getReferenceCode() == null) continue;
            String key = t.getTaskType().name() + ":" + t.getReferenceCode();
            assigned.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(t.getAssignedTo().getName() + (t.getStatus() == DailyTaskStatus.COMPLETED ? " (done)" : ""));
            taken.add(key);
        }
        List<Job> list = new ArrayList<>();

        // 1. Inbound: supplier deliveries (purchase orders) and incoming transfers expected today.
        for (Delivery d : inbound.forWarehouse(w, day, day, today())) {
            if ("RECEIVED".equals(d.status())) continue;
            boolean supplier = "SUPPLIER".equals(d.type());
            String ref = d.receiptCode() != null ? d.receiptCode() : d.orderCode();
            String state = switch (d.status()) {
                case "ARRIVED" -> "goods arrived, ready to receive";
                case "RECEIVING" -> "being received" + (d.receivingStaff() == null ? "" : " by " + d.receivingStaff());
                case "LATE" -> "late, expected " + d.deliveryDate();
                case "IN_TRANSIT" -> "in transit";
                default -> "expected today";
            };
            add(list, assigned, taken, new Job(null, "INBOUND", "RECEIVING", supplier ? "RECEIVING" : "TRANSFER",
                    (supplier ? "Receive & inspect " : "Receive transfer ") + d.orderCode(),
                    (supplier ? "Supplier " : "From ") + d.supplierName() + " · " + d.lines() + " lines / "
                            + d.remainingQuantity() + " units · " + state,
                    ref, "/warehouse/receiving/process", "ARRIVED".equals(d.status()) || "LATE".equals(d.status()) ? "URGENT" : "HIGH",
                    (int) Math.min(Integer.MAX_VALUE, d.remainingQuantity()), d.deliveryDate(), null, null, List.of(), false));
        }

        // 2. Putaway: locked to the staff member who inspected the goods.
        for (PutawayTasks p : openPutaway(w)) {
            String ref = p.getGoodsReceipt() == null ? p.getPutawayTasksCode() : p.getGoodsReceipt().getReceiptCode();
            Users owner = p.getAssignedTo();
            add(list, assigned, taken, new Job(null, "INBOUND", "RECEIVING", "PUTAWAY", "Put away " + ref,
                    owner == null ? "Putaway " + p.getPutawayTasksCode() + " waiting for a staff member"
                            : "Inspected by " + owner.getName() + " – the same person puts it on the shelves (" + p.getPutawayTasksCode() + ")",
                    ref, "/warehouse/staff-putaway", "HIGH", null, null,
                    owner == null ? null : owner.getId(), owner == null ? null : owner.getName(), List.of(), false));
        }

        // 3. Outbound transfers: picking, packing, dispatch.
        for (StockTransfer t : em.createQuery("""
                SELECT t FROM StockTransfer t LEFT JOIN FETCH t.toWarehouse
                WHERE t.fromWarehouse.id = :w AND t.status IN :st ORDER BY t.expectedShipmentDate ASC, t.createdAt ASC
                """, StockTransfer.class).setParameter("w", w)
                .setParameter("st", List.of(TransferStatus.CREATED, TransferStatus.PICKING, TransferStatus.PACKING, TransferStatus.PACKED))
                .setMaxResults(30).getResultList()) {
            String type = switch (t.getStatus()) {
                case PACKING -> "PACKING";
                case PACKED -> "DISPATCH";
                default -> "PICKING";
            };
            String verb = switch (type) { case "PACKING" -> "Pack & seal "; case "DISPATCH" -> "Load & hand over "; default -> "Pick "; };
            String path = switch (type) { case "PACKING" -> "/warehouse/staff-packing"; case "DISPATCH" -> "/warehouse/dispatch"; default -> "/warehouse/picking"; };
            boolean dueToday = t.getExpectedShipmentDate() != null && !t.getExpectedShipmentDate().isAfter(day);
            add(list, assigned, taken, new Job(null, "OUTBOUND", "OUTBOUND", type, verb + t.getTransferCode(),
                    "To " + (t.getToWarehouse() == null ? "—" : t.getToWarehouse().getName()) + " · " + t.getItems().size()
                            + " lines · ship " + (t.getExpectedShipmentDate() == null ? "date not set" : t.getExpectedShipmentDate()),
                    t.getTransferCode(), path, dueToday ? "HIGH" : "NORMAL", t.getItems().size(), t.getExpectedShipmentDate(),
                    null, null, List.of(), false));
        }

        // 4. Damaged goods and shortages.
        for (DamagedGoodsReports d : em.createQuery("""
                SELECT d FROM DamagedGoodsReports d WHERE d.warehouse.id = :w AND d.status IN :st ORDER BY d.createdAt ASC
                """, DamagedGoodsReports.class).setParameter("w", w)
                .setParameter("st", List.of(DamagedGoodsStatus.REPORTED, DamagedGoodsStatus.INSPECTING, DamagedGoodsStatus.APPROVED))
                .setMaxResults(20).getResultList()) {
            add(list, assigned, taken, new Job(null, "ISSUES", "ISSUES", "DAMAGE_HANDLING", "Handle damaged goods " + d.getReportCode(),
                    (d.getDescription() == null ? "Damage report" : d.getDescription()) + " · " + d.getStatus(),
                    d.getReportCode(), "/warehouse/staff-damage", "HIGH", null, null, null, null, List.of(), false));
        }
        for (DiscrepancyReports r : em.createQuery("""
                SELECT r FROM DiscrepancyReports r WHERE r.warehouse.id = :w AND r.status IN :st ORDER BY r.createdAt ASC
                """, DiscrepancyReports.class).setParameter("w", w)
                .setParameter("st", List.of(DiscrepancyStatus.OPEN, DiscrepancyStatus.INVESTIGATING))
                .setMaxResults(20).getResultList()) {
            add(list, assigned, taken, new Job(null, "ISSUES", "ISSUES", "SHORTAGE_HANDLING", "Check shortage " + r.getReportCode(),
                    r.getDiscrepancyType() + (r.getDescription() == null ? "" : " · " + r.getDescription()),
                    r.getReportCode(), "/warehouse/staff-damage", "NORMAL", null, null, null, null, List.of(), false));
        }

        // 5. Physical stock count: first week of the month and mid-month week.
        overview.countWindows(w, day).stream().filter(CountWindow::active).findFirst().ifPresent(window -> {
            List<CountPlan> open = window.plans().stream()
                    .filter(c -> !"COMPLETED".equals(c.status()) && !"CANCELLED".equals(c.status())).toList();
            String label = "MID_MONTH".equals(window.type()) ? "Mid-month" : "First-week";
            if (open.isEmpty()) {
                add(list, assigned, taken, new Job(null, "STOCK_COUNT", "STOCK_COUNT", "STOCK_COUNT", label + " stock count",
                        "Physical count week " + window.start() + " – " + window.end(), "COUNT-" + window.start(),
                        "/warehouse/staff-counts", "URGENT", null, null, null, null, List.of(), false));
            }
            for (CountPlan c : open) {
                add(list, assigned, taken, new Job(null, "STOCK_COUNT", "STOCK_COUNT", "STOCK_COUNT", label + " count " + c.code(),
                        "Physical count week " + window.start() + " – " + window.end() + " · " + c.status().replace('_', ' ')
                                + (c.assignedTo() == null ? "" : " · counter " + c.assignedTo()),
                        c.code(), "/warehouse/staff-counts", "URGENT", null, null, null, null, List.of(), false));
            }
        });

        // 6. Routine arrangement of shelves.
        add(list, assigned, taken, new Job(null, "ISSUES", "ISSUES", "ARRANGEMENT", "Arrange shelves & clear receiving area",
                "Consolidate partial pallets, keep fast movers near dispatch, return misplaced items.",
                "ARRANGE-" + day, "/warehouse/staff-putaway", "LOW", null, null, null, null, List.of(), false));
        return list;
    }

    private static void add(List<Job> list, Map<String, List<String>> assigned, Set<String> taken, Job j) {
        String key = j.taskType() + ":" + j.referenceCode();
        list.add(new Job(key, j.category(), j.role(), j.taskType(), j.title(), j.description(), j.referenceCode(),
                j.referencePath(), j.priority(), j.quantity(), j.expectedDate(), j.lockedStaffId(), j.lockedStaffName(),
                assigned.getOrDefault(key, List.of()), taken.contains(key)));
    }

    // ======================= SHIFT ROLES =======================

    /** Vai tro trong ngay cua tung nhan vien (Manager phan dau ngay). */
    @Transactional(readOnly = true)
    public List<ShiftRow> shifts(LocalDate date) {
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        LocalDate day = date == null ? today() : date;
        Map<Long, List<StaffShiftAssignment>> byStaff = new HashMap<>();
        for (StaffShiftAssignment s : shiftRepository.findForWarehouseDay(w, day)) {
            byStaff.computeIfAbsent(s.getStaff().getId(), k -> new ArrayList<>()).add(s);
        }
        return access.staffOfWarehouse(w).stream().map(u -> {
            List<StaffShiftAssignment> rows = byStaff.getOrDefault(u.getId(), List.of());
            return new ShiftRow(u.getId(), u.getName(), u.getEmail(),
                    rows.stream().map(r -> r.getWorkRole().name()).sorted().toList(),
                    rows.stream().map(StaffShiftAssignment::getNote).filter(Objects::nonNull).findFirst().orElse(null));
        }).toList();
    }

    /** Thay toan bo vai tro cua mot nhan vien trong ngay (danh sach rong = nghi / khong phan vai tro). */
    @Transactional
    public ShiftRow setShift(ShiftRequest r) {
        Users manager = access.currentWarehouseUser();
        if (r == null || r.staffId() == null) throw bad("Select a staff member");
        LocalDate day = r.date() == null ? today() : r.date();
        if (day.isBefore(today())) throw bad("Shift roles cannot be changed in the past");
        Users staff = access.staffOfWarehouse(manager.getWarehouse().getId()).stream()
                .filter(u -> u.getId().equals(r.staffId())).findFirst()
                .orElseThrow(() -> bad("You can only plan shifts for active staff of your warehouse"));
        Set<WorkRole> roles = new TreeSet<>();
        for (String role : r.roles() == null ? List.<String>of() : r.roles()) {
            if (role != null && !role.isBlank()) roles.add(parse(WorkRole.class, role, WorkRole.FLEX, "work role"));
        }
        shiftRepository.deleteForStaffDay(staff.getId(), day);
        for (WorkRole role : roles) {
            shiftRepository.save(StaffShiftAssignment.builder().warehouse(manager.getWarehouse()).staff(staff)
                    .workDate(day).workRole(role).note(limit(r.note(), 300)).assignedBy(manager).build());
        }
        return new ShiftRow(staff.getId(), staff.getName(), staff.getEmail(),
                roles.stream().map(Enum::name).toList(), limit(r.note(), 300));
    }

    /** Sao chep phan vai tro cua mot ngay sang ngay khac. */
    @Transactional
    public List<ShiftRow> copyShifts(CopyShiftRequest r) {
        if (r == null || r.fromDate() == null || r.toDate() == null) throw bad("Select both dates");
        if (r.toDate().isBefore(today())) throw bad("Shift roles cannot be copied into the past");
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        List<StaffShiftAssignment> source = shiftRepository.findForWarehouseDay(w, r.fromDate());
        if (source.isEmpty()) throw bad("No shift roles were planned on " + r.fromDate());
        Map<Long, List<StaffShiftAssignment>> byStaff = new HashMap<>();
        source.forEach(s -> byStaff.computeIfAbsent(s.getStaff().getId(), k -> new ArrayList<>()).add(s));
        Set<Long> active = new HashSet<>();
        access.staffOfWarehouse(w).forEach(u -> active.add(u.getId()));
        for (Map.Entry<Long, List<StaffShiftAssignment>> e : byStaff.entrySet()) {
            if (!active.contains(e.getKey())) continue;
            shiftRepository.deleteForStaffDay(e.getKey(), r.toDate());
            for (StaffShiftAssignment s : e.getValue()) {
                shiftRepository.save(StaffShiftAssignment.builder().warehouse(manager.getWarehouse()).staff(s.getStaff())
                        .workDate(r.toDate()).workRole(s.getWorkRole()).note(s.getNote()).assignedBy(manager).build());
            }
        }
        return shifts(r.toDate());
    }

    // ======================= STAFF SELF-SERVICE =======================

    /** Nhan vien: vai tro hom nay va cac viec dung vai tro chua co ai nhan (hoac viec bat buoc cua minh). */
    @Transactional(readOnly = true)
    public StaffQueue staffQueue(LocalDate date) {
        Users staff = access.currentWarehouseUser();
        LocalDate day = date == null ? today() : date;
        Set<String> roles = rolesOf(staff.getId(), day);
        List<Job> jobs = jobsFor(staff.getWarehouse().getId(), day).stream()
                .filter(j -> canTake(j, staff, roles) && !j.taken())
                .toList();
        return new StaffQueue(day, roles.stream().sorted().toList(), jobs);
    }

    /** Nhan vien tu nhan mot viec trong vai tro cua minh: tao viec trong ngay o trang thai dang lam. */
    @Transactional
    public Task takeJob(TakeJobRequest r) {
        Users staff = access.currentWarehouseUser();
        LocalDate day = r == null || r.date() == null ? today() : r.date();
        if (!day.equals(today())) throw bad("You can only take work for today");
        if (r == null || r.jobKey() == null) throw bad("Select a job");
        Set<String> roles = rolesOf(staff.getId(), day);
        if (roles.isEmpty()) throw bad("Your manager has not assigned you a role for today yet");
        Job job = jobsFor(staff.getWarehouse().getId(), day).stream().filter(j -> r.jobKey().equals(j.key())).findFirst()
                .orElseThrow(() -> conflict("This job is no longer available. Refresh the list."));
        if (!canTake(job, staff, roles)) {
            throw bad(job.lockedStaffId() != null
                    ? "Only " + job.lockedStaffName() + " can do this job (they inspected these goods)"
                    : "This job belongs to the " + job.role() + " role, which you do not have today");
        }
        if (job.taken()) throw conflict("Someone has already taken this job");
        DailyTaskType type = parse(DailyTaskType.class, job.taskType(), DailyTaskType.OTHER, "task type");
        if (type == DailyTaskType.PUTAWAY) requireReceiverForPutaway(staff.getWarehouse().getId(), job.referenceCode(), staff);
        if (type == DailyTaskType.RECEIVING) requireSameReceiver(staff.getWarehouse().getId(), job.referenceCode(), staff);
        LocalDateTime now = LocalDateTime.now(WarehouseOperationsOverviewService.ZONE);
        WarehouseDailyTask t = tasks.save(WarehouseDailyTask.builder()
                .taskCode(nextCode(day)).warehouse(staff.getWarehouse()).assignedTo(staff).assignedBy(staff)
                .taskDate(day).taskType(type).title(limit(job.title(), 200)).description(limit(job.description(), 1000))
                .priority(parse(DailyTaskPriority.class, job.priority(), DailyTaskPriority.NORMAL, "priority"))
                .referenceCode(limit(job.referenceCode(), 100)).referencePath(job.referencePath())
                .targetQuantity(job.quantity()).managerNote("Self-assigned from the work queue")
                .status(DailyTaskStatus.IN_PROGRESS).startedAt(now).build());
        return view(t);
    }

    private Set<String> rolesOf(Long staffId, LocalDate day) {
        Set<String> roles = new HashSet<>();
        shiftRepository.findForStaffDay(staffId, day).forEach(s -> roles.add(s.getWorkRole().name()));
        return roles;
    }

    private static boolean canTake(Job j, Users staff, Set<String> roles) {
        if (j.lockedStaffId() != null) return j.lockedStaffId().equals(staff.getId());
        return roles.contains("FLEX") || roles.contains(j.role());
    }

    // ======================= STAFF AVAILABILITY =======================

    /**
     * Nhan vien ranh hay ban:
     *  BUSY   = dang lam (viec trong ngay IN_PROGRESS, dang giu nhan hang / cat hang / lay hang, dang kiem ke)
     *  LOADED = chua lam nhung con >= 3 viec mo
     *  FREE   = khong co viec dang lam
     */
    @Transactional(readOnly = true)
    public List<StaffAvailability> availability(LocalDate date) {
        Users manager = access.currentWarehouseUser();
        Long w = manager.getWarehouse().getId();
        LocalDate day = date == null ? today() : date;
        Map<Long, List<WarehouseDailyTask>> byStaff = new HashMap<>();
        for (WarehouseDailyTask t : tasks.findForWarehouseDay(w, day)) {
            byStaff.computeIfAbsent(t.getAssignedTo().getId(), k -> new ArrayList<>()).add(t);
        }
        Map<Long, List<String>> claims = new HashMap<>();
        for (WarehouseTaskClaim c : em.createQuery("""
                SELECT c FROM WarehouseTaskClaim c JOIN FETCH c.claimedBy u
                WHERE c.releasedAt IS NULL AND u.warehouse.id = :w
                """, WarehouseTaskClaim.class).setParameter("w", w).getResultList()) {
            claims.computeIfAbsent(c.getClaimedBy().getId(), k -> new ArrayList<>()).add(claimLabel(c));
        }
        for (StockCounts c : em.createQuery("""
                SELECT c FROM StockCounts c JOIN FETCH c.assignedTo
                WHERE c.warehouse.id = :w AND c.status IN :st
                """, StockCounts.class).setParameter("w", w)
                .setParameter("st", List.of(StockCountStatus.COUNTING, StockCountStatus.RECOUNTING)).getResultList()) {
            claims.computeIfAbsent(c.getAssignedTo().getId(), k -> new ArrayList<>()).add("Counting " + c.getCountCode());
        }
        Map<Long, List<String>> roles = new HashMap<>();
        for (StaffShiftAssignment s : shiftRepository.findForWarehouseDay(w, day)) {
            roles.computeIfAbsent(s.getStaff().getId(), k -> new ArrayList<>()).add(s.getWorkRole().name());
        }
        List<StaffAvailability> result = new ArrayList<>();
        for (Users u : access.staffOfWarehouse(w)) {
            List<WarehouseDailyTask> own = byStaff.getOrDefault(u.getId(), List.of());
            long inProgress = own.stream().filter(t -> t.getStatus() == DailyTaskStatus.IN_PROGRESS).count();
            long open = own.stream().filter(t -> OPEN.contains(t.getStatus())).count();
            long done = own.stream().filter(t -> t.getStatus() == DailyTaskStatus.COMPLETED).count();
            List<String> work = new ArrayList<>();
            own.stream().filter(t -> t.getStatus() == DailyTaskStatus.IN_PROGRESS).forEach(t -> work.add(t.getTitle()));
            work.addAll(claims.getOrDefault(u.getId(), List.of()));
            String status = !work.isEmpty() ? "BUSY" : open >= 3 ? "LOADED" : "FREE";
            LocalTime nextDue = own.stream().filter(t -> OPEN.contains(t.getStatus()) && t.getDueTime() != null)
                    .map(WarehouseDailyTask::getDueTime).min(Comparator.naturalOrder()).orElse(null);
            result.add(new StaffAvailability(u.getId(), u.getName(), u.getEmail(), status, open, inProgress, done, work, nextDue,
                    roles.getOrDefault(u.getId(), List.of()).stream().sorted().toList()));
        }
        result.sort(Comparator.comparing((StaffAvailability a) -> switch (a.status()) { case "FREE" -> 0; case "LOADED" -> 1; default -> 2; })
                .thenComparingLong(StaffAvailability::openTasks).thenComparing(StaffAvailability::name));
        return result;
    }

    private String claimLabel(WarehouseTaskClaim c) {
        Long id = c.getReferenceId();
        return switch (c.getTaskType()) {
            case GOODS_RECEIVING -> "Receiving " + code("SELECT g.receiptCode FROM GoodsReceipts g WHERE g.id = :id", id, "receipt #" + id);
            case PUTAWAY -> "Putaway " + code("SELECT p.putawayTasksCode FROM PutawayTasks p WHERE p.id = :id", id, "#" + id);
            case TRANSFER_PICKING -> "Picking " + code("SELECT t.transferCode FROM StockTransfer t WHERE t.id = :id", id, "#" + id);
            case TRANSFER_PACKING -> "Packing " + code("SELECT t.transferCode FROM StockTransfer t WHERE t.id = :id", id, "#" + id);
            case TRANSFER_RECEIVING -> "Receiving transfer " + code("SELECT t.transferCode FROM StockTransfer t WHERE t.id = :id", id, "#" + id);
            case STORE_RETURN_RECEIVING -> "Receiving store return #" + id;
            case STOCK_COUNT -> "Counting " + code("SELECT c.countCode FROM StockCounts c WHERE c.id = :id", id, "#" + id);
        };
    }

    private String code(String jpql, Long id, String fallback) {
        return em.createQuery(jpql, String.class).setParameter("id", id).getResultStream().findFirst().orElse(fallback);
    }

    // ======================= RULE: RECEIVER = PUTAWAY =======================

    private List<PutawayTasks> openPutaway(Long w) {
        return em.createQuery("""
                SELECT p FROM PutawayTasks p LEFT JOIN FETCH p.assignedTo LEFT JOIN FETCH p.goodsReceipt
                WHERE p.warehouse.id = :w AND p.status IN :st ORDER BY p.createdAt ASC
                """, PutawayTasks.class)
                .setParameter("w", w)
                .setParameter("st", List.of(PutawayTaskStatus.PENDING, PutawayTaskStatus.AVAILABLE, PutawayTaskStatus.IN_PROGRESS))
                .setMaxResults(20).getResultList();
    }

    /**
     * Nhan vien kiem hang (nhan hang) cua phieu nao thi phai cat hang cua phieu do.
     * Ap dung khi giao viec PUTAWAY co ma tham chieu (ma phieu nhap GR-... hoac ma putaway PA-...).
     */
    private void requireReceiverForPutaway(Long warehouseId, String referenceCode, Users staff) {
        if (referenceCode == null || referenceCode.isBlank()) return;
        String ref = referenceCode.trim();
        Users owner = em.createQuery("""
                SELECT p FROM PutawayTasks p LEFT JOIN FETCH p.assignedTo LEFT JOIN p.goodsReceipt g
                WHERE p.warehouse.id = :w AND (p.putawayTasksCode = :ref OR g.receiptCode = :ref)
                ORDER BY p.createdAt DESC
                """, PutawayTasks.class)
                .setParameter("w", warehouseId).setParameter("ref", ref).setMaxResults(1)
                .getResultStream().findFirst().map(PutawayTasks::getAssignedTo).orElse(null);
        if (owner == null) {
            // Phieu chua duoc duyet: nguoi dang/da nhan hang (claim GOODS_RECEIVING moi nhat).
            owner = em.createQuery("""
                    SELECT c.claimedBy FROM WarehouseTaskClaim c, GoodsReceipts g
                    WHERE c.taskType = :t AND c.referenceId = g.id AND g.receiptCode = :ref AND g.warehouse.id = :w
                    ORDER BY c.claimedAt DESC
                    """, Users.class)
                    .setParameter("t", WarehouseTaskType.GOODS_RECEIVING).setParameter("ref", ref)
                    .setParameter("w", warehouseId).setMaxResults(1)
                    .getResultStream().findFirst().orElse(null);
        }
        if (owner != null && !owner.getId().equals(staff.getId())) {
            throw bad("Goods in " + ref + " were received and inspected by " + owner.getName()
                    + ". The same staff member must put them away.");
        }
    }

    /** Phieu nhap dang duoc mot nhan vien kiem thi khong giao viec kiem phieu do cho nguoi khac. */
    private void requireSameReceiver(Long warehouseId, String referenceCode, Users staff) {
        if (referenceCode == null || referenceCode.isBlank()) return;
        Users owner = em.createQuery("""
                SELECT c.claimedBy FROM WarehouseTaskClaim c, GoodsReceipts g
                WHERE c.taskType = :t AND c.referenceId = g.id AND g.receiptCode = :ref
                  AND g.warehouse.id = :w AND c.releasedAt IS NULL
                ORDER BY c.claimedAt DESC
                """, Users.class)
                .setParameter("t", WarehouseTaskType.GOODS_RECEIVING).setParameter("ref", referenceCode.trim())
                .setParameter("w", warehouseId).setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
        if (owner != null && !owner.getId().equals(staff.getId())) {
            throw bad(referenceCode.trim() + " is already being inspected by " + owner.getName()
                    + ". That person will also put the goods away.");
        }
    }

    /** Giao viec nhan hang thi tu dong giao luon viec cat hang len ke cho cung nhan vien. */
    private WarehouseDailyTask addFollowUpPutaway(Users manager, WarehouseDailyTask receiving, TaskRequest r) {
        if (receiving.getTaskType() != DailyTaskType.RECEIVING) return null;
        if (r != null && Boolean.FALSE.equals(r.includePutaway())) return null;
        String ref = receiving.getReferenceCode();
        return tasks.save(WarehouseDailyTask.builder()
                .taskCode(nextCode(receiving.getTaskDate()))
                .warehouse(receiving.getWarehouse()).assignedTo(receiving.getAssignedTo()).assignedBy(manager)
                .taskDate(receiving.getTaskDate()).taskType(DailyTaskType.PUTAWAY)
                .title(limit("Put away " + (ref == null ? "the goods you received" : ref), 200))
                .description("After the manager confirms the receipt, put the accepted goods on the shelves. "
                        + "The staff member who inspected the goods also puts them away.")
                .priority(receiving.getPriority())
                .referenceCode(ref).referencePath("/warehouse/staff-putaway")
                .targetQuantity(receiving.getTargetQuantity())
                .status(DailyTaskStatus.ASSIGNED).build());
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
