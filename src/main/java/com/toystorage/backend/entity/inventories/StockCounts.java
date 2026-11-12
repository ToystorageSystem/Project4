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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockCounts {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "count_code", nullable = false, unique = true, length = 50)
    private String countCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;
    @Enumerated(EnumType.STRING)
    @Column(name = "count_type", nullable = false, length = 30)
    private StockCountType countType;
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;
    @Enumerated(EnumType.STRING)
    @Builder.Default @Column(name = "status", nullable = false, length = 30)
    private StockCountStatus status = StockCountStatus.PLANNED;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Users confirmedBy;
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private Users assignedTo;
    @Column(name = "stock_counts_code", nullable = false, unique = true, length = 255)
    private String stockCountsCode;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Null for ad-hoc plans; database uniqueness makes scheduled creation idempotent.
    @Column(name = "periodic_key", unique = true, length = 100)
    private String periodicKey;
    // ALL, ZONE, LOCATION or PRODUCT; selected before COUNTING.
    @Builder.Default @Column(name = "scope_type", length = 20)
    private String scopeType = "ALL";
    @Column(name = "scope_value", length = 100)
    private String scopeValue;

    @PrePersist protected void onCreate() {
        if (status == null) status = StockCountStatus.PLANNED;
        if (scopeType == null) scopeType = "ALL";
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
