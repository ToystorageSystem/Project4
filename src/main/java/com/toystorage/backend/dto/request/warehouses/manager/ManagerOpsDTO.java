package com.toystorage.backend.dto.request.warehouses.manager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** DTO cho cac chuc nang Warehouse Manager: phan cong, hieu suat, sap het hang, tong quan van hanh. */
public final class ManagerOpsDTO {
    private ManagerOpsDTO() {}

    // ================= DAILY TASKS =================
    public record StaffOption(Long id, String code, String name, String email) {}

    public record TaskRequest(Long staffId, LocalDate taskDate, String taskType, String title,
                              String description, String priority, LocalTime dueTime,
                              String referenceCode, String referencePath, Integer targetQuantity,
                              String managerNote) {}

    public record BulkTaskRequest(List<Long> staffIds, TaskRequest task) {}

    public record CopyTasksRequest(LocalDate fromDate, LocalDate toDate) {}

    public record StaffProgressRequest(Integer completedQuantity, String note) {}

    public record CancelRequest(String reason) {}

    public record Task(Long id, String code, LocalDate taskDate, String taskType, String title,
                       String description, String priority, String status, LocalTime dueTime,
                       String referenceCode, String referencePath, Integer targetQuantity,
                       Integer completedQuantity, Long assignedToId, String assignedTo,
                       String assignedBy, String staffNote, String managerNote,
                       LocalDateTime startedAt, LocalDateTime completedAt, boolean overdue, boolean onTime) {}

    public record StaffDay(Long staffId, String staffName, String email, long total, long completed,
                           long inProgress, long overdue, List<Task> tasks) {}

    public record DayBoard(LocalDate date, long total, long completed, long inProgress, long assigned,
                           long overdue, List<StaffDay> staff, List<Suggestion> suggestions) {}

    public record Suggestion(String taskType, String title, String description, String referencePath,
                             String priority, long pendingCount) {}

    // ================= PERFORMANCE =================
    public record ReviewRequest(LocalDate periodFrom, LocalDate periodTo, Integer rating,
                                String strengths, String improvements, String comment) {}

    public record Review(Long id, String code, Long staffId, String staffName, String reviewer,
                         LocalDate periodFrom, LocalDate periodTo, Integer rating,
                         BigDecimal systemScore, String grade, String strengths,
                         String improvements, String comment, LocalDateTime createdAt) {}

    public record Metrics(long tasksAssigned, long tasksCompleted, long tasksOnTime, long tasksOverdue,
                          long tasksCancelled, double completionRate, double onTimeRate,
                          double avgCompletionMinutes, long receiptsHandled, long putawayCompleted,
                          long stockCountsCompleted, long packingCompleted, long damageReports,
                          long claimedWarehouseTasks, Map<String, Long> tasksByType) {}

    public record StaffPerformance(Long staffId, String code, String name, String email,
                                   Metrics metrics, Double averageRating, BigDecimal score,
                                   String grade, String remark, Review latestReview) {}

    public record PerformanceBoard(LocalDate from, LocalDate to, int staffCount, BigDecimal averageScore,
                                   List<StaffPerformance> staff) {}

    public record StaffPerformanceDetail(StaffPerformance summary, List<Task> recentTasks,
                                         List<Review> reviews, List<DailyPoint> daily) {}

    public record DailyPoint(LocalDate date, long assigned, long completed) {}

    // ================= LOW STOCK =================
    public record LowStockLine(Long productId, String productCode, String productName, String barcode,
                               String unit, long onHand, long reserved, long available,
                               int minimumLevel, boolean defaultMinimum, int suggestedQuantity,
                               String severity, int locations) {}

    public record LowStockPreview(Long warehouseId, String warehouseName, int defaultThreshold,
                                  long outOfStock, long critical, long low, List<LowStockLine> items) {}

    public record MinimumRequest(Integer minimum) {}

    public record SendLowStockRequest(List<Long> productIds, Integer threshold, String note,
                                      Map<Long, Integer> suggestedQuantities) {}

    public record LowStockReportSummary(Long id, String code, Long warehouseId, String warehouseName,
                                        String createdBy, String status, int itemCount,
                                        long outOfStock, long critical, String managerNote,
                                        String businessNote, String handledBy,
                                        LocalDateTime createdAt, LocalDateTime acknowledgedAt,
                                        LocalDateTime resolvedAt) {}

    public record LowStockReportItemView(Long productId, String productCode, String productName,
                                         int onHand, int reserved, int available, int minimumLevel,
                                         int suggestedQuantity, String severity) {}

    public record LowStockReportDetail(LowStockReportSummary report, List<LowStockReportItemView> items) {}

    public record BusinessHandleRequest(String note) {}

    // ================= OPERATIONS OVERVIEW =================
    public record Movement(Long id, String code, String direction, String counterpart, String status,
                           String transferType, LocalDate expectedDate, LocalDateTime createdAt) {}

    public record CountWindow(String type, LocalDate start, LocalDate end, boolean active,
                              boolean upcoming, List<CountPlan> plans) {}

    public record CountPlan(Long id, String code, String type, String status, LocalDate scheduledDate,
                            String assignedTo) {}

    public record Overview(LocalDate today,
                           Map<String, Long> goodsReceipts,
                           Map<String, Long> inboundTransfers,
                           Map<String, Long> outboundTransfers,
                           Map<String, Long> putawayTasks,
                           Map<String, Long> damagedGoods,
                           Map<String, Long> discrepancies,
                           Map<String, Long> stockCounts,
                           List<Movement> recentMovements,
                           List<CountWindow> countWindows,
                           long lowStockProducts) {}
}
