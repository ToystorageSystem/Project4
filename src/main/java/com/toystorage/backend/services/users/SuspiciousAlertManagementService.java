package com.toystorage.backend.services.users;

import com.toystorage.backend.dto.request.users.ResolveSuspiciousAlertRequest;
import com.toystorage.backend.dto.response.users.SuspiciousAlertResponse;

import com.toystorage.backend.entity.users.SuspiciousActivityAlerts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.enums.users.SuspiciousAlertStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.users.SuspiciousActivityAlertRepository;
import com.toystorage.backend.repository.users.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuspiciousAlertManagementService {

    private final SuspiciousActivityAlertRepository
            suspiciousActivityAlertRepository;

    private final UserRepository
            userRepository;

    private final SuspiciousAlertQueryService
            suspiciousAlertQueryService;

    private final ActivityLogService
            activityLogService;


    // =========================================================
    // START INVESTIGATION
    //
    // NEW -> INVESTIGATING
    // =========================================================

    @Transactional
    public SuspiciousAlertResponse investigate(

            Long alertId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SuspiciousActivityAlerts alert =
                getAlert(
                        alertId
                );


        // =====================================================
        // STATUS VALIDATION
        // =====================================================

        if (
                alert.getStatus()
                        == SuspiciousAlertStatus.INVESTIGATING
        ) {

            throw new BadRequest(
                    "Cảnh báo đang được kiểm tra"
            );
        }


        if (
                alert.getStatus()
                        == SuspiciousAlertStatus.RESOLVED
                        || alert.getStatus()
                        == SuspiciousAlertStatus.FALSE_POSITIVE
        ) {

            throw new BadRequest(
                    "Cảnh báo đã hoàn tất xử lý"
            );
        }


        if (
                alert.getStatus()
                        != SuspiciousAlertStatus.NEW
        ) {

            throw new BadRequest(
                    "Trạng thái cảnh báo không hợp lệ để bắt đầu điều tra"
            );
        }


        // =====================================================
        // ADMIN ACTOR
        // =====================================================

        Users reviewer =
                getCurrentUser(
                        authentication
                );


        // =====================================================
        // SNAPSHOT BEFORE
        // =====================================================

        Map<String, Object> oldValue =
                snapshot(
                        alert
                );


        // =====================================================
        // UPDATE
        // =====================================================

        LocalDateTime now =
                LocalDateTime.now();


        alert.setStatus(
                SuspiciousAlertStatus.INVESTIGATING
        );


        alert.setReviewedBy(
                reviewer
        );


        alert.setReviewedAt(
                now
        );


        SuspiciousActivityAlerts saved =
                suspiciousActivityAlertRepository
                        .save(
                                alert
                        );


        // =====================================================
        // SNAPSHOT AFTER
        // =====================================================

        Map<String, Object> newValue =
                snapshot(
                        saved
                );


        // =====================================================
        // AUDIT LOG
        // =====================================================

        activityLogService.log(

                authentication,

                httpRequest,

                ActivityAction.UPDATE,

                ActivityEntityType.SUSPICIOUS_ALERT,

                saved.getId(),

                oldValue,

                newValue
        );


        return suspiciousAlertQueryService
                .toResponse(
                        saved
                );
    }


    // =========================================================
    // RESOLVE
    //
    // INVESTIGATING -> RESOLVED
    // =========================================================

    @Transactional
    public SuspiciousAlertResponse resolve(

            Long alertId,

            ResolveSuspiciousAlertRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        return completeAlert(

                alertId,

                request,

                SuspiciousAlertStatus.RESOLVED,

                authentication,

                httpRequest
        );
    }


    // =========================================================
    // FALSE POSITIVE
    //
    // INVESTIGATING -> FALSE_POSITIVE
    // =========================================================

    @Transactional
    public SuspiciousAlertResponse markFalsePositive(

            Long alertId,

            ResolveSuspiciousAlertRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        return completeAlert(

                alertId,

                request,

                SuspiciousAlertStatus.FALSE_POSITIVE,

                authentication,

                httpRequest
        );
    }


    // =========================================================
    // COMPLETE ALERT
    // =========================================================

    private SuspiciousAlertResponse completeAlert(

            Long alertId,

            ResolveSuspiciousAlertRequest request,

            SuspiciousAlertStatus targetStatus,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SuspiciousActivityAlerts alert =
                getAlert(
                        alertId
                );


        // =====================================================
        // CHỈ ĐƯỢC HOÀN TẤT KHI ĐANG INVESTIGATING
        // =====================================================

        if (
                alert.getStatus()
                        == SuspiciousAlertStatus.RESOLVED
                        || alert.getStatus()
                        == SuspiciousAlertStatus.FALSE_POSITIVE
        ) {

            throw new BadRequest(
                    "Cảnh báo đã hoàn tất xử lý"
            );
        }


        if (
                alert.getStatus()
                        != SuspiciousAlertStatus.INVESTIGATING
        ) {

            throw new BadRequest(
                    "Cảnh báo phải ở trạng thái INVESTIGATING trước khi hoàn tất"
            );
        }


        // =====================================================
        // NOTE REQUIRED
        // =====================================================

        String note =
                request.getResolutionNote();


        if (
                note == null
                        || note.trim().isEmpty()
        ) {

            throw new BadRequest(
                    "Ghi chú xử lý không được để trống"
            );
        }


        // =====================================================
        // VERIFY CURRENT ADMIN
        // =====================================================

        getCurrentUser(
                authentication
        );


        // =====================================================
        // SNAPSHOT BEFORE
        // =====================================================

        Map<String, Object> oldValue =
                snapshot(
                        alert
                );


        // =====================================================
        // UPDATE
        // =====================================================

        alert.setStatus(
                targetStatus
        );


        alert.setResolutionNote(
                note.trim()
        );


        alert.setResolvedAt(
                LocalDateTime.now()
        );


        SuspiciousActivityAlerts saved =
                suspiciousActivityAlertRepository
                        .save(
                                alert
                        );


        // =====================================================
        // SNAPSHOT AFTER
        // =====================================================

        Map<String, Object> newValue =
                snapshot(
                        saved
                );


        // =====================================================
        // AUDIT HISTORY
        // =====================================================

        activityLogService.log(

                authentication,

                httpRequest,

                ActivityAction.UPDATE,

                ActivityEntityType.SUSPICIOUS_ALERT,

                saved.getId(),

                oldValue,

                newValue
        );


        return suspiciousAlertQueryService
                .toResponse(
                        saved
                );
    }


    // =========================================================
    // GET ALERT
    // =========================================================

    private SuspiciousActivityAlerts getAlert(
            Long alertId
    ) {

        return suspiciousActivityAlertRepository
                .findById(
                        alertId
                )
                .orElseThrow(
                        () ->
                                new NotFound(
                                        "Không tìm thấy cảnh báo hoạt động bất thường"
                                )
                );
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private Users getCurrentUser(
            Authentication authentication
    ) {

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            throw new Unauthorized(
                    "Bạn chưa đăng nhập"
            );
        }


        String email =
                authentication.getName();


        return userRepository
                .findByEmail(
                        email
                )
                .orElseThrow(
                        () ->
                                new NotFound(
                                        "Không tìm thấy người xử lý cảnh báo"
                                )
                );
    }


    // =========================================================
    // SNAPSHOT
    //
    // Dùng để lưu toàn bộ lịch sử xử lý vào activity_logs.
    // =========================================================

    private Map<String, Object> snapshot(
            SuspiciousActivityAlerts alert
    ) {

        Map<String, Object> value =
                new LinkedHashMap<>();


        value.put(
                "alertId",
                alert.getId()
        );


        value.put(
                "alertCode",
                alert.getSuspiciousActivityAlertCode()
        );


        value.put(
                "status",
                alert.getStatus() != null
                        ? alert.getStatus().name()
                        : null
        );


        value.put(
                "riskScore",
                alert.getRiskScore()
        );


        value.put(
                "alertLevel",
                alert.getAlertLevel() != null
                        ? alert.getAlertLevel().name()
                        : null
        );


        value.put(
                "userId",
                alert.getUser() != null
                        ? alert.getUser().getId()
                        : null
        );


        value.put(
                "reviewedById",
                alert.getReviewedBy() != null
                        ? alert.getReviewedBy().getId()
                        : null
        );


        value.put(
                "reviewedByEmail",
                alert.getReviewedBy() != null
                        ? alert.getReviewedBy().getEmail()
                        : null
        );


        value.put(
                "reviewedAt",
                alert.getReviewedAt()
        );


        value.put(
                "resolutionNote",
                alert.getResolutionNote()
        );


        value.put(
                "resolvedAt",
                alert.getResolvedAt()
        );


        return value;
    }
}