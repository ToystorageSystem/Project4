package com.toystorage.backend.services.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.toystorage.backend.dto.response.users.ActivityLogResponse;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;


    // =========================================================
    // NORMAL ACTIVITY LOG
    //
    // Dùng cho các thao tác đã đăng nhập:
    //
    // CREATE
    // UPDATE
    // DELETE
    // GRANT_PERMISSION
    // LOCK_USER
    // ...
    // =========================================================

    @Transactional
    public ActivityLogs log(

            Authentication authentication,

            HttpServletRequest httpRequest,

            ActivityAction action,

            ActivityEntityType entityType,

            Long entityId,

            Object oldValue,

            Object newValue
    ) {

        // =====================================================
        // 1. ACTOR
        // =====================================================

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            throw new IllegalStateException(
                    "Không xác định được người thực hiện thao tác"
            );
        }


        String email =
                authentication.getName();


        Users actor =
                userRepository
                        .findByEmail(
                                email
                        )
                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Không tìm thấy người thực hiện thao tác"
                                        )
                        );


        // =====================================================
        // 2. SAVE
        // =====================================================

        return saveLog(

                actor,

                httpRequest,

                action,

                entityType,

                entityId,

                oldValue,

                newValue
        );
    }


    // =========================================================
    // LOGIN / LOGIN FAILED
    //
    // REQUIRES_NEW:
    //
    // LOGIN_FAILED vẫn được lưu ngay cả khi AuthService
    // sau đó throw Unauthorized.
    // =========================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public ActivityLogs logAuthenticationAttempt(

            String attemptedEmail,

            HttpServletRequest httpRequest,

            ActivityAction action
    ) {

        // =====================================================
        // Chỉ method này mới dùng cho LOGIN / LOGIN_FAILED
        // =====================================================

        if (
                action != ActivityAction.LOGIN
                        && action != ActivityAction.LOGIN_FAILED
        ) {

            throw new IllegalArgumentException(
                    "Authentication log chỉ hỗ trợ LOGIN hoặc LOGIN_FAILED"
            );
        }


        // =====================================================
        // EMAIL CÓ THỂ KHÔNG TỒN TẠI
        //
        // Login thất bại với email giả:
        //
        // actor = null
        // =====================================================

        String normalizedEmail =
                attemptedEmail != null
                        ? attemptedEmail.trim()
                        : null;


        Users actor =
                normalizedEmail == null
                        || normalizedEmail.isBlank()

                        ? null

                        : userRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElse(
                                null
                        );


        // =====================================================
        // LOGIN DETAIL
        //
        // TUYỆT ĐỐI không chứa password.
        // =====================================================

        Map<String, Object> detail =
                new LinkedHashMap<>();


        detail.put(
                "attemptedEmail",
                normalizedEmail
        );


        detail.put(
                "result",

                action == ActivityAction.LOGIN
                        ? "SUCCESS"
                        : "FAILED"
        );


        return saveLog(

                actor,

                httpRequest,

                action,

                ActivityEntityType.AUTHENTICATION,

                actor != null
                        ? actor.getId()
                        : null,

                null,

                detail
        );
    }


    // =========================================================
    // SAVE LOG
    // =========================================================

    private ActivityLogs saveLog(

            Users actor,

            HttpServletRequest httpRequest,

            ActivityAction action,

            ActivityEntityType entityType,

            Long entityId,

            Object oldValue,

            Object newValue
    ) {

        String ipAddress =
                getClientIp(
                        httpRequest
                );


        String deviceInfo =
                getDeviceInfo(
                        httpRequest
                );


        ActivityLogs log =
                ActivityLogs
                        .builder()

                        .activityLogsCode(
                                generateLogCode()
                        )

                        .user(
                                actor
                        )

                        .action(
                                action
                        )

                        .entityType(
                                entityType
                        )

                        .entityId(
                                entityId
                        )

                        .oldValue(
                                toSafeJson(
                                        oldValue
                                )
                        )

                        .newValue(
                                toSafeJson(
                                        newValue
                                )
                        )

                        .ipAddress(
                                ipAddress
                        )

                        .deviceInfo(
                                deviceInfo
                        )

                        .build();


        return activityLogRepository
                .save(
                        log
                );
    }


    // =========================================================
    // USER HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getUserHistory(
            Long userId
    ) {

        if (
                !userRepository.existsById(
                        userId
                )
        ) {

            throw new NotFound(
                    "Không tìm thấy tài khoản"
            );
        }


        return activityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(

                        ActivityEntityType.USER,

                        userId
                )

                .stream()

                .map(
                        this::toResponse
                )

                .toList();
    }


    // =========================================================
    // RESPONSE
    // =========================================================

    private ActivityLogResponse toResponse(
            ActivityLogs log
    ) {

        Users actor =
                log.getUser();


        return ActivityLogResponse
                .builder()

                .id(
                        log.getId()
                )

                .activityLogCode(
                        log.getActivityLogsCode()
                )

                .actorUserId(
                        actor != null
                                ? actor.getId()
                                : null
                )

                .actorUserCode(
                        actor != null
                                ? actor.getUserCode()
                                : null
                )

                .actorName(
                        actor != null
                                ? actor.getName()
                                : null
                )

                .actorEmail(
                        actor != null
                                ? actor.getEmail()
                                : null
                )

                .action(
                        log.getAction() != null
                                ? log.getAction().name()
                                : null
                )

                .entityType(
                        log.getEntityType() != null
                                ? log.getEntityType().name()
                                : null
                )

                .entityId(
                        log.getEntityId()
                )

                .oldValue(
                        log.getOldValue()
                )

                .newValue(
                        log.getNewValue()
                )

                .ipAddress(
                        log.getIpAddress()
                )

                .deviceInfo(
                        log.getDeviceInfo()
                )

                .createdAt(
                        log.getCreatedAt()
                )

                .build();
    }


    // =========================================================
    // SAFE JSON
    //
    // Trước khi lưu oldValue/newValue:
    //
    // password
    // token
    // secret
    // authorization
    // cookie
    // apiKey
    // sessionId
    //
    // sẽ được thay bằng [REDACTED].
    // =========================================================

    private String toSafeJson(
            Object value
    ) {

        if (value == null) {

            return null;
        }


        try {

            JsonNode jsonNode =
                    objectMapper
                            .valueToTree(
                                    value
                            );


            sanitizeJsonNode(
                    jsonNode
            );


            return objectMapper
                    .writeValueAsString(
                            jsonNode
                    );

        } catch (
                IllegalArgumentException
                        | JsonProcessingException ex
        ) {

            throw new IllegalStateException(
                    "Không thể chuyển dữ liệu Activity Log sang JSON",
                    ex
            );
        }
    }


    // =========================================================
    // SANITIZE JSON RECURSIVELY
    // =========================================================

    private void sanitizeJsonNode(
            JsonNode node
    ) {

        if (node == null) {
            return;
        }


        // =====================================================
        // OBJECT
        // =====================================================

        if (node.isObject()) {

            ObjectNode objectNode =
                    (ObjectNode) node;


            List<String> fieldNames =
                    new ArrayList<>();


            objectNode
                    .fieldNames()
                    .forEachRemaining(
                            fieldNames::add
                    );


            for (String fieldName : fieldNames) {

                JsonNode child =
                        objectNode.get(
                                fieldName
                        );


                if (
                        isSensitiveKey(
                                fieldName
                        )
                ) {

                    objectNode.put(
                            fieldName,
                            "[REDACTED]"
                    );

                } else {

                    sanitizeJsonNode(
                            child
                    );
                }
            }


            return;
        }


        // =====================================================
        // ARRAY
        // =====================================================

        if (node.isArray()) {

            for (JsonNode child : node) {

                sanitizeJsonNode(
                        child
                );
            }
        }
    }


    // =========================================================
    // SENSITIVE FIELD DETECTION
    // =========================================================

    private boolean isSensitiveKey(
            String key
    ) {

        if (key == null) {

            return false;
        }


        String normalized =
                key
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );


        return normalized.contains(
                "password"
        )

                || normalized.contains(
                "token"
        )

                || normalized.contains(
                "secret"
        )

                || normalized.contains(
                "apikey"
        )

                || normalized.contains(
                "authorization"
        )

                || normalized.contains(
                "cookie"
        )

                || normalized.contains(
                "sessionid"
        )

                || normalized.contains(
                "jsessionid"
        )

                || normalized.equals(
                "credential"
        )

                || normalized.equals(
                "credentials"
        );
    }


    // =========================================================
    // CLIENT IP
    // =========================================================

    private String getClientIp(
            HttpServletRequest request
    ) {

        if (request == null) {

            return null;
        }


        String forwardedFor =
                request.getHeader(
                        "X-Forwarded-For"
                );


        if (
                forwardedFor != null
                        && !forwardedFor.isBlank()
        ) {

            String ip =
                    forwardedFor
                            .split(",")[0]
                            .trim();


            if (ip.length() > 45) {

                return ip.substring(
                        0,
                        45
                );
            }


            return ip;
        }


        String ip =
                request.getRemoteAddr();


        if (
                ip != null
                        && ip.length() > 45
        ) {

            return ip.substring(
                    0,
                    45
            );
        }


        return ip;
    }


    // =========================================================
    // DEVICE
    // =========================================================

    private String getDeviceInfo(
            HttpServletRequest request
    ) {

        if (request == null) {

            return null;
        }


        String deviceInfo =
                request.getHeader(
                        "User-Agent"
                );


        if (
                deviceInfo != null
                        && deviceInfo.length() > 500
        ) {

            return deviceInfo.substring(
                    0,
                    500
            );
        }


        return deviceInfo;
    }


    // =========================================================
    // LOG CODE
    // =========================================================

    private String generateLogCode() {

        return "ACT-"
                + UUID
                .randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .toUpperCase();
    }
}