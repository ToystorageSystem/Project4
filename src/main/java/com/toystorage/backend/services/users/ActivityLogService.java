package com.toystorage.backend.services.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;


    // =========================================================
    // GHI ACTIVITY LOG
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
        // 1. XÁC ĐỊNH NGƯỜI THỰC HIỆN
        // =====================================================

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            throw new IllegalStateException(
                    "Không xác định được người thực hiện thao tác"
            );
        }


        /*
         * authentication.getName()
         * hiện tại chính là email đăng nhập.
         */
        String email =
                authentication.getName();


        Users actor =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Không tìm thấy người thực hiện thao tác"
                                )
                        );


        // =====================================================
        // 2. LẤY IP
        // =====================================================

        String ipAddress =
                getClientIp(httpRequest);


        // =====================================================
        // 3. LẤY THÔNG TIN THIẾT BỊ
        // =====================================================

        String deviceInfo = null;

        if (httpRequest != null) {

            deviceInfo =
                    httpRequest.getHeader(
                            "User-Agent"
                    );


            if (
                    deviceInfo != null
                            && deviceInfo.length() > 500
            ) {

                deviceInfo =
                        deviceInfo.substring(
                                0,
                                500
                        );
            }
        }


        // =====================================================
        // 4. TẠO LOG
        // =====================================================

        ActivityLogs log =
                ActivityLogs
                        .builder()

                        .activityLogsCode(
                                generateLogCode()
                        )

                        /*
                         * user = người thực hiện.
                         */
                        .user(actor)

                        .action(action)

                        .entityType(entityType)

                        /*
                         * entityId = đối tượng bị thao tác.
                         *
                         * Ví dụ:
                         *
                         * Admin 12 lock User 15:
                         *
                         * user_id   = 12
                         * entity_id = 15
                         */
                        .entityId(entityId)

                        .oldValue(
                                toJson(oldValue)
                        )

                        .newValue(
                                toJson(newValue)
                        )

                        .ipAddress(
                                ipAddress
                        )

                        .deviceInfo(
                                deviceInfo
                        )

                        .build();


        return activityLogRepository.save(
                log
        );
    }


    // =========================================================
    // XEM LỊCH SỬ CỦA USER
    // =========================================================

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getUserHistory(
            Long userId
    ) {

        /*
         * Đảm bảo User tồn tại.
         */
        if (!userRepository.existsById(userId)) {

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
    // MAPPER
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
    // JSON
    // =========================================================

    private String toJson(
            Object value
    ) {

        if (value == null) {
            return null;
        }


        try {

            return objectMapper
                    .writeValueAsString(
                            value
                    );

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "Không thể chuyển dữ liệu Activity Log sang JSON",
                    ex
            );
        }
    }


    // =========================================================
    // IP ADDRESS
    // =========================================================

    private String getClientIp(
            HttpServletRequest request
    ) {

        if (request == null) {
            return null;
        }


        /*
         * Khi deploy qua proxy/nginx,
         * IP thật thường nằm ở đây.
         */
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
                return ip.substring(0, 45);
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
    // GENERATE LOG CODE
    // =========================================================

    private String generateLogCode() {

        /*
         * Ví dụ:
         *
         * ACT-AFB4221213514CE6A22A8CECBF547166
         *
         * Không phụ thuộc ID DB.
         */
        return "ACT-"
                + UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }
}