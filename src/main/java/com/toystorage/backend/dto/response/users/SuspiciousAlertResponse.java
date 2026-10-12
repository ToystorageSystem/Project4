package com.toystorage.backend.dto.response.users;

import com.toystorage.backend.enums.users.SuspiciousAlertLevel;
import com.toystorage.backend.enums.users.SuspiciousAlertStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspiciousAlertResponse {

    // =========================================================
    // ALERT
    // =========================================================

    private Long id;

    private String alertCode;

    private BigDecimal riskScore;

    private SuspiciousAlertLevel alertLevel;

    private SuspiciousAlertStatus status;

    private String description;


    // =========================================================
    // RELATED USER
    // =========================================================

    private Long userId;

    private String userCode;

    private String userName;

    private String userEmail;


    // =========================================================
    // RELATED ACTIVITY LOG
    // =========================================================

    private Long relatedLogId;

    private String relatedLogCode;

    private String relatedLogAction;

    private String relatedLogEntityType;

    private LocalDateTime relatedLogCreatedAt;


    // =========================================================
    // REVIEWER
    // =========================================================

    private Long reviewedById;

    private String reviewedByUserCode;

    private String reviewedByName;

    private String reviewedByEmail;


    // =========================================================
    // PROCESSING INFORMATION
    // =========================================================

    private String resolutionNote;

    private LocalDateTime detectedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime updatedAt;
}