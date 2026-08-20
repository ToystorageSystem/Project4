package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.StockCountType;
import com.toystorage.backend.enums.inventories.StockCountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Mã phiếu kiểm kê. */
    @Column(name = "count_code", nullable = false, unique = true, length = 50)
    private String countCode;

    /* Kho hoặc Store được kiểm kê. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    /* Loại kiểm kê, ví dụ PERIODIC, MONTH_END, AD_HOC. */
    @Enumerated(EnumType.STRING)
    @Column(name = "count_type", nullable = false, length = 30)
    private StockCountType countType;

    /* Ngày dự kiến kiểm kê. */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    /* Trạng thái phiếu kiểm kê. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private StockCountStatus status = StockCountStatus.PLANNED;

    /* Người tạo kế hoạch kiểm kê. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    /* Người duyệt kết quả kiểm kê. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Users confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /* Thời điểm bắt đầu kiểm kê. */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /* Thời điểm hoàn tất kiểm kê. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private Users assignedTo;

    /* Mã nội bộ theo đúng database. */
    @Column(name = "stock_counts_code", nullable = false, unique = true, length = 255)
    private String stockCountsCode;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StockCountStatus.PLANNED;
        }

        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
