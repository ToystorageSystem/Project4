package com.toystorage.backend.dto.response.users;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityLogResponse {

    private Long id;

    private String activityLogCode;

    /*
     * Người THỰC HIỆN thao tác.
     */
    private Long actorUserId;

    private String actorUserCode;

    private String actorName;

    private String action;

    private String entityType;

    /*
     * User bị tác động.
     */
    private Long entityId;

    private String oldValue;

    private String newValue;

    private String ipAddress;

    private String deviceInfo;

    private LocalDateTime createdAt;
}