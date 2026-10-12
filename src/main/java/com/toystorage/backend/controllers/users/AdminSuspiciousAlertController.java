package com.toystorage.backend.controllers.users;

import com.toystorage.backend.dto.request.users.ResolveSuspiciousAlertRequest;

import com.toystorage.backend.dto.response.users.ActivityLogResponse;
import com.toystorage.backend.dto.response.users.SuspiciousAlertResponse;

import com.toystorage.backend.enums.users.SuspiciousAlertLevel;
import com.toystorage.backend.enums.users.SuspiciousAlertStatus;

import com.toystorage.backend.services.users.ActivityLogQueryService;
import com.toystorage.backend.services.users.SuspiciousAlertManagementService;
import com.toystorage.backend.services.users.SuspiciousAlertQueryService;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suspicious-alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSuspiciousAlertController {

    private final SuspiciousAlertQueryService
            suspiciousAlertQueryService;

    private final SuspiciousAlertManagementService
            suspiciousAlertManagementService;

    private final ActivityLogQueryService
            activityLogQueryService;


    // =========================================================
    // LIST + SEARCH + FILTER + PAGINATION
    // =========================================================

    @GetMapping
    public ResponseEntity<Page<SuspiciousAlertResponse>>
    getAlerts(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            SuspiciousAlertLevel alertLevel,

            @RequestParam(required = false)
            SuspiciousAlertStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        Pageable pageable =
                PageRequest.of(

                        page,

                        size,

                        Sort.by(

                                Sort.Order.desc(
                                        "detectedAt"
                                ),

                                Sort.Order.desc(
                                        "id"
                                )
                        )
                );


        Page<SuspiciousAlertResponse> result =
                suspiciousAlertQueryService
                        .getAlerts(

                                keyword,

                                alertLevel,

                                status,

                                fromDate,

                                toDate,

                                pageable
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // DETAIL
    // =========================================================

    @GetMapping("/{alertId}")
    public ResponseEntity<SuspiciousAlertResponse>
    getAlertById(

            @PathVariable
            Long alertId
    ) {

        SuspiciousAlertResponse result =
                suspiciousAlertQueryService
                        .getAlertById(
                                alertId
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // USER ACTIVITY HISTORY
    //
    // Lấy toàn bộ Activity Log của User
    // liên quan đến cảnh báo.
    //
    // alertId
    //      ↓
    // alert.userId
    //      ↓
    // activity_logs
    // =========================================================

    @GetMapping("/{alertId}/user-history")
    public ResponseEntity<List<ActivityLogResponse>>
    getUserActivityHistory(

            @PathVariable
            Long alertId
    ) {

        // -----------------------------------------------------
        // 1. GET ALERT
        // -----------------------------------------------------

        SuspiciousAlertResponse alert =
                suspiciousAlertQueryService
                        .getAlertById(
                                alertId
                        );


        // -----------------------------------------------------
        // 2. GET ACTIVITY HISTORY OF RELATED USER
        // -----------------------------------------------------

        List<ActivityLogResponse> result =
                activityLogQueryService
                        .getUserActivityHistory(
                                alert.getUserId()
                        );


        // -----------------------------------------------------
        // 3. RESPONSE
        // -----------------------------------------------------

        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // START INVESTIGATION
    //
    // NEW -> INVESTIGATING
    // =========================================================

    @PatchMapping("/{alertId}/investigating")
    public ResponseEntity<SuspiciousAlertResponse>
    investigate(

            @PathVariable
            Long alertId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SuspiciousAlertResponse result =
                suspiciousAlertManagementService
                        .investigate(

                                alertId,

                                authentication,

                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // RESOLVE
    //
    // INVESTIGATING -> RESOLVED
    // =========================================================

    @PatchMapping("/{alertId}/resolved")
    public ResponseEntity<SuspiciousAlertResponse>
    resolve(

            @PathVariable
            Long alertId,

            @Valid
            @RequestBody
            ResolveSuspiciousAlertRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SuspiciousAlertResponse result =
                suspiciousAlertManagementService
                        .resolve(

                                alertId,

                                request,

                                authentication,

                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // FALSE POSITIVE
    //
    // INVESTIGATING -> FALSE_POSITIVE
    // =========================================================

    @PatchMapping("/{alertId}/false-positive")
    public ResponseEntity<SuspiciousAlertResponse>
    markFalsePositive(

            @PathVariable
            Long alertId,

            @Valid
            @RequestBody
            ResolveSuspiciousAlertRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        SuspiciousAlertResponse result =
                suspiciousAlertManagementService
                        .markFalsePositive(

                                alertId,

                                request,

                                authentication,

                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }
}