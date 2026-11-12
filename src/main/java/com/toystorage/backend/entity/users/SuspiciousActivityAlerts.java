package com.toystorage.backend.entity.users;

import com.toystorage.backend.enums.users.SuspiciousAlertStatus;
import com.toystorage.backend.enums.users.SuspiciousAlertLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
@Entity
@Table(
        name = "suspicious_activity_alerts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_suspicious_activity_alerts_code",
                columnNames = "suspicious_activity_alerts_code"
        ),
        indexes = {
                @Index(
                        name = "idx_suspicious_alerts_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_suspicious_alerts_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_suspicious_alerts_detected_at",
                        columnList = "detected_at"
                ),
                @Index(
                        name = "idx_suspicious_alerts_level",
                        columnList = "alert_level"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspiciousActivityAlerts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(
            name = "risk_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal riskScore;

    @Lob
    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_log_id")
    private ActivityLogs relatedLog;
    /*
     * Trạng thái xử lý cảnh báo.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private SuspiciousAlertStatus status =
            SuspiciousAlertStatus.NEW;
    /*
     * Mức độ nghiêm trọng của cảnh báo.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "alert_level",
            nullable = false,
            length = 20
    )

    private SuspiciousAlertLevel alertLevel =
            SuspiciousAlertLevel.MEDIUM;
    /*
     * Ghi chú xử lý của Admin.
     */
    @Lob
    @Column(
            name = "resolution_note",
            columnDefinition = "TEXT"
    )
    private String resolutionNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Users reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(
            name = "suspicious_activity_alerts_code",
            nullable = false,
            length = 50
    )
    private String suspiciousActivityAlertCode;
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (detectedAt == null) {
            detectedAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = SuspiciousAlertStatus.NEW;
        }

        if (alertLevel == null) {
            alertLevel = SuspiciousAlertLevel.MEDIUM;
        }

    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
