package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.receipts.ReceivingIncidentSourceDecision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "receiving_incident_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receiving_incident_receipt_source",
                columnNames = {"goods_receipt_id", "source_decision"}
        ),
        indexes = @Index(
                name = "idx_receiving_incident_report_warehouse_created",
                columnList = "warehouse_id, created_at"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingIncidentReports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã nội bộ của entity.
     * Ví dụ: RIR-A1B2C3D4E5F6
     */
    @Column(
            name = "receiving_incident_reports_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String receivingIncidentReportsCode;

    /*
     * Mã biên bản hiển thị cho người dùng.
     * Ví dụ: BB-WH-EA4EF26FBF14
     */
    @Column(
            name = "report_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String reportCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipts goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_staff_id", nullable = false)
    private Users warehouseStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_manager_id", nullable = false)
    private Users warehouseManager;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_decision", nullable = false, length = 30)
    private ReceivingIncidentSourceDecision sourceDecision;

    @Column(name = "manager_note", columnDefinition = "TEXT")
    private String managerNote;

    @Column(name = "penalty_action", columnDefinition = "TEXT")
    private String penaltyAction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "report",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<ReceivingIncidentReportItems> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (receivingIncidentReportsCode == null
                || receivingIncidentReportsCode.isBlank()) {

            receivingIncidentReportsCode =
                    "RIR-"
                            + UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 12)
                            .toUpperCase();
        }

        if (reportCode == null || reportCode.isBlank()) {

            reportCode =
                    "BB-WH-"
                            + UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 12)
                            .toUpperCase();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(ReceivingIncidentReportItems item) {
        items.add(item);
        item.setReport(this);
    }
}