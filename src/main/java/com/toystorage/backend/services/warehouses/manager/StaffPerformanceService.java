package com.toystorage.backend.services.warehouses.manager;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.StaffPerformanceReview;
import com.toystorage.backend.entity.warehouses.WarehouseDailyTask;
import com.toystorage.backend.enums.warehouses.DailyTaskStatus;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.manager.StaffPerformanceReviewRepository;
import com.toystorage.backend.repository.warehouses.manager.WarehouseDailyTaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.toystorage.backend.dto.request.warehouses.manager.ManagerOpsDTO.*;

/**
 * Danh gia hieu suat nhan vien kho.
 * Diem he thong (0-100):
 *   45% ty le hoan thanh cong viec duoc giao
 *   25% ty le hoan thanh dung han
 *   15% nang suat thao tac tren he thong (nhan hang, cat hang, kiem ke, dong goi, bao hong...) so voi nguoi cao nhat
 *   15% diem danh gia cua quan ly (1-5)
 * Thanh phan nao chua co du lieu se duoc loai ra va chia lai trong so.
 */
@Service
@RequiredArgsConstructor
public class StaffPerformanceService {

    @PersistenceContext
    private EntityManager em;

    private final ManagerOpsAccess access;
    private final WarehouseDailyTaskRepository taskRepository;
    private final StaffPerformanceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final DailyTaskService dailyTaskService;

    @Transactional(readOnly = true)
    public PerformanceBoard board(LocalDate from, LocalDate to) {
        Users manager = access.currentWarehouseUser();
        Range range = range(from, to);
        Long w = manager.getWarehouse().getId();
        List<Users> staff = access.staffOfWarehouse(w);
        Map<Long, List<WarehouseDailyTask>> tasksByStaff = taskRepository.findForWarehouseRange(w, range.from, range.to)
                .stream().collect(Collectors.groupingBy(t -> t.getAssignedTo().getId()));
        Map<Long, List<StaffPerformanceReview>> reviewsByStaff = reviewRepository.findForWarehouse(w)
                .stream().collect(Collectors.groupingBy(r -> r.getStaff().getId()));

        Map<Long, Metrics> metrics = new LinkedHashMap<>();
        for (Users u : staff) metrics.put(u.getId(), metrics(u, tasksByStaff.getOrDefault(u.getId(), List.of()), range));
        long maxActivity = metrics.values().stream().mapToLong(StaffPerformanceService::activity).max().orElse(0);

        List<StaffPerformance> result = new ArrayList<>();
        for (Users u : staff) {
            result.add(summary(u, metrics.get(u.getId()), reviewsByStaff.getOrDefault(u.getId(), List.of()), range, maxActivity));
        }
        result.sort(Comparator.comparing(StaffPerformance::score, Comparator.nullsLast(Comparator.reverseOrder())));
        BigDecimal avg = result.isEmpty() ? BigDecimal.ZERO : result.stream().map(StaffPerformance::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(result.size()), 1, RoundingMode.HALF_UP);
        return new PerformanceBoard(range.from, range.to, result.size(), avg, result);
    }

    @Transactional(readOnly = true)
    public StaffPerformanceDetail detail(Long staffId, LocalDate from, LocalDate to) {
        Users manager = access.currentWarehouseUser();
        Users staff = staffOf(manager, staffId);
        Range range = range(from, to);
        Long w = manager.getWarehouse().getId();
        List<WarehouseDailyTask> all = taskRepository.findForWarehouseRange(w, range.from, range.to);
        long maxActivity = 0;
        Map<Long, List<WarehouseDailyTask>> byStaff = all.stream().collect(Collectors.groupingBy(t -> t.getAssignedTo().getId()));
        for (Users u : access.staffOfWarehouse(w)) {
            maxActivity = Math.max(maxActivity, activity(metrics(u, byStaff.getOrDefault(u.getId(), List.of()), range)));
        }
        List<WarehouseDailyTask> own = byStaff.getOrDefault(staff.getId(), List.of());
        List<StaffPerformanceReview> reviews = reviewRepository.findForStaff(staff.getId());
        StaffPerformance summary = summary(staff, metrics(staff, own, range), reviews, range, maxActivity);

        List<Task> recent = own.stream()
                .sorted(Comparator.comparing(WarehouseDailyTask::getTaskDate).reversed().thenComparing(WarehouseDailyTask::getId, Comparator.reverseOrder()))
                .limit(50).map(dailyTaskService::view).toList();
        List<DailyPoint> daily = new ArrayList<>();
        for (LocalDate d = range.from; !d.isAfter(range.to); d = d.plusDays(1)) {
            LocalDate day = d;
            List<WarehouseDailyTask> dayTasks = own.stream().filter(t -> t.getTaskDate().equals(day)
                    && t.getStatus() != DailyTaskStatus.CANCELLED).toList();
            daily.add(new DailyPoint(day, dayTasks.size(),
                    dayTasks.stream().filter(t -> t.getStatus() == DailyTaskStatus.COMPLETED).count()));
        }
        return new StaffPerformanceDetail(summary, recent, reviews.stream().map(this::review).toList(), daily);
    }

    @Transactional
    public Review addReview(Long staffId, ReviewRequest request) {
        Users manager = access.currentWarehouseUser();
        Users staff = staffOf(manager, staffId);
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw bad("Rating must be between 1 and 5");
        }
        Range range = range(request.periodFrom(), request.periodTo());
        if (isBlank(request.comment()) && isBlank(request.strengths()) && isBlank(request.improvements())) {
            throw bad("Add a comment, strengths or areas to improve");
        }
        PerformanceBoard board = board(range.from, range.to);
        StaffPerformance current = board.staff().stream().filter(s -> s.staffId().equals(staff.getId())).findFirst().orElse(null);
        StaffPerformanceReview saved = reviewRepository.save(StaffPerformanceReview.builder()
                .reviewCode("SPR-" + LocalDate.now(WarehouseOperationsOverviewService.ZONE).format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-" + staff.getId() + "-" + Long.toString(System.currentTimeMillis() % 10_000_000L, 36).toUpperCase(Locale.ROOT))
                .warehouse(manager.getWarehouse()).staff(staff).reviewer(manager)
                .periodFrom(range.from).periodTo(range.to).rating(request.rating())
                .systemScore(current == null ? null : current.score())
                .grade(current == null ? null : current.grade())
                .strengths(limit(request.strengths())).improvements(limit(request.improvements()))
                .comment(limit(request.comment()))
                .build());
        return review(saved);
    }

    // ===================== INTERNAL =====================

    private Metrics metrics(Users u, List<WarehouseDailyTask> tasks, Range range) {
        List<WarehouseDailyTask> active = tasks.stream().filter(t -> t.getStatus() != DailyTaskStatus.CANCELLED).toList();
        long assigned = active.size();
        long completed = active.stream().filter(t -> t.getStatus() == DailyTaskStatus.COMPLETED).count();
        long onTime = active.stream().filter(WarehouseDailyTask::isOnTime).count();
        long overdue = active.stream().filter(t -> t.getStatus() != DailyTaskStatus.COMPLETED && DailyTaskService.isPastDeadline(t)).count()
                + active.stream().filter(t -> t.getStatus() == DailyTaskStatus.COMPLETED && !t.isOnTime()).count();
        long cancelled = tasks.size() - assigned;
        double avgMinutes = active.stream()
                .filter(t -> t.getStartedAt() != null && t.getCompletedAt() != null)
                .mapToLong(t -> Math.max(0, Duration.between(t.getStartedAt(), t.getCompletedAt()).toMinutes()))
                .average().orElse(0);
        Map<String, Long> byType = new TreeMap<>(active.stream().collect(Collectors.groupingBy(t -> t.getTaskType().name(), Collectors.counting())));

        LocalDateTime a = range.from.atStartOfDay();
        LocalDateTime b = range.to.plusDays(1).atStartOfDay();
        Long id = u.getId();
        return new Metrics(assigned, completed, onTime, overdue, cancelled,
                assigned == 0 ? 0 : round(completed * 100.0 / assigned),
                completed == 0 ? 0 : round(onTime * 100.0 / completed),
                round(avgMinutes),
                count("SELECT COUNT(g) FROM GoodsReceipts g WHERE g.receivedBy.id = :u AND g.receivedAt >= :a AND g.receivedAt < :b", id, a, b),
                count("SELECT COUNT(p) FROM PutawayTasks p WHERE p.assignedTo.id = :u AND p.completedAt >= :a AND p.completedAt < :b", id, a, b),
                count("SELECT COUNT(c) FROM StockCounts c WHERE c.assignedTo.id = :u AND c.completedAt >= :a AND c.completedAt < :b", id, a, b),
                count("SELECT COUNT(t) FROM StockTransfer t WHERE t.packingCompletedBy.id = :u AND t.packingCompletedAt >= :a AND t.packingCompletedAt < :b", id, a, b),
                count("SELECT COUNT(d) FROM DamagedGoodsReports d WHERE d.reportedBy.id = :u AND d.createdAt >= :a AND d.createdAt < :b", id, a, b),
                count("SELECT COUNT(c) FROM WarehouseTaskClaim c WHERE c.claimedBy.id = :u AND c.claimedAt >= :a AND c.claimedAt < :b", id, a, b),
                byType);
    }

    private static long activity(Metrics m) {
        return m.tasksCompleted() + m.receiptsHandled() + m.putawayCompleted() + m.stockCountsCompleted()
                + m.packingCompleted() + m.damageReports() + m.claimedWarehouseTasks();
    }

    private StaffPerformance summary(Users u, Metrics m, List<StaffPerformanceReview> reviews, Range range, long maxActivity) {
        List<StaffPerformanceReview> inPeriod = reviews.stream()
                .filter(r -> !r.getPeriodTo().isBefore(range.from) && !r.getPeriodFrom().isAfter(range.to)).toList();
        Double avgRating = inPeriod.isEmpty() ? null
                : round(inPeriod.stream().mapToInt(StaffPerformanceReview::getRating).average().orElse(0));

        double weighted = 0, weights = 0;
        if (m.tasksAssigned() > 0) {
            weighted += 45 * m.completionRate() / 100.0; weights += 45;
            if (m.tasksCompleted() > 0) { weighted += 25 * m.onTimeRate() / 100.0; weights += 25; }
        }
        if (maxActivity > 0) { weighted += 15 * (double) activity(m) / maxActivity; weights += 15; }
        if (avgRating != null) { weighted += 15 * avgRating / 5.0; weights += 15; }
        BigDecimal score = weights == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(weighted * 100 / weights).setScale(1, RoundingMode.HALF_UP);
        String grade = weights == 0 ? "N/A" : grade(score.doubleValue());

        StaffPerformanceReview latest = reviews.stream().max(Comparator.comparing(StaffPerformanceReview::getCreatedAt)).orElse(null);
        return new StaffPerformance(u.getId(), u.getUserCode(), u.getName(), u.getEmail(), m, avgRating, score, grade,
                remark(m, grade, weights == 0), latest == null ? null : review(latest));
    }

    private static String grade(double score) {
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        return "D";
    }

    private static String remark(Metrics m, String grade, boolean noData) {
        if (noData) return "No assigned tasks or warehouse activity in this period.";
        List<String> notes = new ArrayList<>();
        switch (grade) {
            case "A" -> notes.add("Excellent performance");
            case "B" -> notes.add("Good performance");
            case "C" -> notes.add("Meets part of the expectations");
            default -> notes.add("Needs close follow-up");
        }
        if (m.tasksAssigned() > 0 && m.completionRate() < 70) notes.add("low task completion (" + m.completionRate() + "%)");
        if (m.tasksCompleted() > 0 && m.onTimeRate() < 70) notes.add("often late (" + m.onTimeRate() + "% on time)");
        if (m.tasksOverdue() > 0) notes.add(m.tasksOverdue() + " overdue task(s)");
        if (m.damageReports() > 0) notes.add(m.damageReports() + " damage report(s) filed");
        return String.join(" · ", notes);
    }

    private long count(String jpql, Long userId, LocalDateTime a, LocalDateTime b) {
        try {
            return em.createQuery(jpql, Long.class).setParameter("u", userId).setParameter("a", a).setParameter("b", b).getSingleResult();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private Review review(StaffPerformanceReview r) {
        return new Review(r.getId(), r.getReviewCode(), r.getStaff().getId(), r.getStaff().getName(), r.getReviewer().getName(),
                r.getPeriodFrom(), r.getPeriodTo(), r.getRating(), r.getSystemScore(), r.getGrade(),
                r.getStrengths(), r.getImprovements(), r.getComment(), r.getCreatedAt());
    }

    private Users staffOf(Users manager, Long staffId) {
        Users staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
        boolean ok = access.staffOfWarehouse(manager.getWarehouse().getId()).stream().anyMatch(s -> s.getId().equals(staff.getId()));
        if (!ok) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found");
        return staff;
    }

    private record Range(LocalDate from, LocalDate to) {}

    private static Range range(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(WarehouseOperationsOverviewService.ZONE);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end)) throw bad("The start date must be before the end date");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw bad("The period cannot exceed one year");
        return new Range(start, end);
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }

    private static boolean isBlank(String v) { return v == null || v.isBlank(); }

    private static String limit(String v) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        return t.length() > 1000 ? t.substring(0, 1000) : t;
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
