package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.ResolutionAction;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "discrepancy_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyReports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã báo cáo chênh lệch. */
    @Column(name = "report_code", nullable = false, unique = true, length = 50)
    private String reportCode;

    /** Loại chứng từ nguồn: GOODS_RECEIPT, STORE_RECEIPT, STOCK_COUNT... */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 50)
    private DiscrepancyReferenceType referenceType;

    /** ID của chứng từ nguồn. */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;
    /**
     * Product phát sinh discrepancy.
     * Với GOODS_RECEIPT, mỗi product có một report riêng.
     */
    @Column(name = "product_id")
    private Long productId;
    /** Kho/cửa hàng phát sinh chênh lệch. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    /** SHORTAGE, SURPLUS, DAMAGED hoặc OTHER. */
    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private DiscrepancyType discrepancyType;

    /** OPEN, UNDER_REVIEW, RESOLVED, REJECTED. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private DiscrepancyStatus status = DiscrepancyStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private Users reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Users reviewedBy;

    @Column(name = "responsible_party", length = 100)
    private String responsibleParty;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** KEEP_SURPLUS, RETURN_SURPLUS, ADJUST_INVENTORY, RECOUNT, REJECT, OTHER. */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 30)
    private ResolutionAction resolutionAction;

    @Lob
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Users resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "evidence_image_url", length = 500)
    private String evidenceImageUrl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "discrepancy_reports_code", nullable = false, length = 50)
    private String discrepancyReportsCode;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) {
            status = DiscrepancyStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
