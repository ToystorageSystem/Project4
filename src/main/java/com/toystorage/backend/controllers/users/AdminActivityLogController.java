package com.toystorage.backend.controllers.users;

import com.toystorage.backend.dto.response.users.ActivityLogResponse;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.services.users.ActivityLogExportService;
import com.toystorage.backend.services.users.ActivityLogQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
public class AdminActivityLogController {

    private final ActivityLogQueryService activityLogQueryService;

    private final ActivityLogExportService activityLogExportService;


    // =========================================================
    // LIST + SEARCH + FILTER + PAGINATION
    // =========================================================

    @GetMapping
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('AUDIT_READ')"
    )
    public ResponseEntity<Page<ActivityLogResponse>>
    getActivityLogs(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            ActivityAction action,

            @RequestParam(required = false)
            ActivityEntityType entityType,

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
                                        "createdAt"
                                ),

                                Sort.Order.desc(
                                        "id"
                                )
                        )
                );


        Page<ActivityLogResponse> result =
                activityLogQueryService
                        .getActivityLogs(
                                keyword,
                                action,
                                entityType,
                                fromDate,
                                toDate,
                                pageable
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // EXPORT CSV
    // =========================================================

    @GetMapping(
            value = "/export",
            produces = "text/csv"
    )
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('AUDIT_READ')"
    )
    public ResponseEntity<byte[]> exportActivityLogs(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            ActivityAction action,

            @RequestParam(required = false)
            ActivityEntityType entityType,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime toDate
    ) {

        byte[] csv =
                activityLogExportService
                        .exportCsv(
                                keyword,
                                action,
                                entityType,
                                fromDate,
                                toDate
                        );


        String timestamp =
                LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        );


        String filename =
                "activity-logs-"
                        + timestamp
                        + ".csv";


        return ResponseEntity
                .ok()

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + filename
                                + "\""
                )

                .contentType(
                        MediaType.parseMediaType(
                                "text/csv;charset=UTF-8"
                        )
                )

                .body(
                        csv
                );
    }


    // =========================================================
    // DETAIL
    // =========================================================

    @GetMapping("/{logId}")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('AUDIT_READ')"
    )
    public ResponseEntity<ActivityLogResponse>
    getActivityLogById(

            @PathVariable
            Long logId
    ) {

        ActivityLogResponse result =
                activityLogQueryService
                        .getActivityLogById(
                                logId
                        );


        return ResponseEntity.ok(
                result
        );
    }
}