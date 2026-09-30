package com.toystorage.backend.dto.response.users;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ActivityLogResponse {

    private Long id;

    private String activityLogCode;


    // =========================================================
    // ACTOR
    // =========================================================

    private Long actorUserId;

    private String actorUserCode;

    private String actorName;

    private String actorEmail;

    private List<String> actorRoles;


    // =========================================================
    // ACTION
    // =========================================================

    private String action;


    // =========================================================
    // TARGET ENTITY
    // =========================================================

    private String entityType;

    private Long entityId;


    // =========================================================
    // CHANGE DATA
    // =========================================================

    private String oldValue;

    private String newValue;


    // =========================================================
    // CLIENT INFORMATION
    // =========================================================

    private String ipAddress;

    private String deviceInfo;


    // =========================================================
    // TIME
    // =========================================================

    private LocalDateTime createdAt;
}