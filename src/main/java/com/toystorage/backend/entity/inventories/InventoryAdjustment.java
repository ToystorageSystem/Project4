package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.inventories.InventoryAdjustmentStatus;
import com.toystorage.backend.entity.warehouses.Warehouses;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã phiếu điều chỉnh tồn. */
    @Column(name = "adjustment_code", nullable = false, unique = true, length = 50)
    private String adjustmentCode;

    /** Báo cáo chênh lệch làm căn cứ điều chỉnh, có thể không có. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discrepancy_report_id")
    private DiscrepancyReports discrepancyReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    /** PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private InventoryAdjustmentStatus status = InventoryAdjustmentStatus.PENDING;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Users approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "inventory_adjustments_code", nullable = false, unique = true, length = 255)
    private String inventoryAdjustmentsCode;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) {
            status = InventoryAdjustmentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
