package com.toystorage.backend.entity.users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_logs_user_id", columnList = "user_id"),
                @Index(name = "idx_activity_logs_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_activity_logs_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "activity_logs_code",
            nullable = false,
            length = 50
    )
    private String activityLogsCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private ActivityEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Lob
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Lob
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (activityLogsCode == null || activityLogsCode.isBlank()) {
            activityLogsCode =
                    "AL-"
                            + UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 16)
                            .toUpperCase();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}