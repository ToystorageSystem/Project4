package com.toystorage.backend.entity.users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(
                        name = "idx_activity_logs_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_activity_logs_entity",
                        columnList = "entity_type, entity_id"
                ),
                @Index(
                        name = "idx_activity_logs_created_at",
                        columnList = "created_at"
                )
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


    // =========================================================
    // LOG CODE
    // =========================================================

    @Column(
            name = "activity_logs_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String activityLogsCode;


    // =========================================================
    // ACTOR
    //
    // Có thể NULL trong trường hợp LOGIN_FAILED
    // với email không tồn tại trong hệ thống.
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = true
    )
    private Users user;


    // =========================================================
    // ACTION
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            length = 100
    )
    private ActivityAction action;


    // =========================================================
    // ENTITY
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "entity_type",
            length = 100
    )
    private ActivityEntityType entityType;


    @Column(
            name = "entity_id"
    )
    private Long entityId;


    // =========================================================
    // OLD / NEW VALUE
    // =========================================================

    @Lob
    @Column(
            name = "old_value",
            columnDefinition = "TEXT"
    )
    private String oldValue;


    @Lob
    @Column(
            name = "new_value",
            columnDefinition = "TEXT"
    )
    private String newValue;


    // =========================================================
    // CLIENT INFORMATION
    // =========================================================

    @Column(
            name = "ip_address",
            length = 45
    )
    private String ipAddress;


    @Column(
            name = "device_info",
            length = 500
    )
    private String deviceInfo;


    // =========================================================
    // CREATED AT
    // =========================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {

            createdAt =
                    LocalDateTime.now();
        }
    }
}