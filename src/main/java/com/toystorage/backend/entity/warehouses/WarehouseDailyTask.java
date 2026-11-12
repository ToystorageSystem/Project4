package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.DailyTaskPriority;
import com.toystorage.backend.enums.warehouses.DailyTaskStatus;
import com.toystorage.backend.enums.warehouses.DailyTaskType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cong viec trong ngay do Warehouse Manager phan cong cho Warehouse Staff.
 * Bang moi, duoc Hibernate tao tu dong (ddl-auto=update).
 */
@Entity
@Table(name = "warehouse_daily_tasks", indexes = {
        @Index(name = "idx_wdt_warehouse_date", columnList = "warehouse_id, task_date"),
        @Index(name = "idx_wdt_assignee_date", columnList = "assigned_to, task_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarehouseDailyTask {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false, unique = true, length = 50)
    private String taskCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_to", nullable = false)
    private Users assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private Users assignedBy;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private DailyTaskType taskType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "priority", nullable = false, length = 20)
    private DailyTaskPriority priority = DailyTaskPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private DailyTaskStatus status = DailyTaskStatus.ASSIGNED;

    /** Han hoan thanh trong ngay (tuy chon). */
    @Column(name = "due_time")
    private LocalTime dueTime;

    /** Ma chung tu lien quan (GR-..., ST-..., SC-..., khu vuc A1...). */
    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    /** Trang FE de nhan vien mo nhanh (vd /warehouse/staff-putaway). */
    @Column(name = "reference_path", length = 200)
    private String referencePath;

    /** So luong muc tieu (dong / san pham / vi tri) - dung de do nang suat. */
    @Column(name = "target_quantity")
    private Integer targetQuantity;

    @Column(name = "completed_quantity")
    private Integer completedQuantity;

    @Column(name = "staff_note", length = 1000)
    private String staffNote;

    @Column(name = "manager_note", length = 1000)
    private String managerNote;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = DailyTaskStatus.ASSIGNED;
        if (priority == null) priority = DailyTaskPriority.NORMAL;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    /** Hoan thanh dung han neu khong co han, hoac hoan thanh truoc han trong ngay giao viec. */
    public boolean isOnTime() {
        if (status != DailyTaskStatus.COMPLETED || completedAt == null) return false;
        LocalDateTime deadline = taskDate.atTime(dueTime == null ? LocalTime.of(23, 59, 59) : dueTime);
        return !completedAt.isAfter(deadline);
    }
}
