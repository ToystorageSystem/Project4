package com.toystorage.backend.services.users;

import com.toystorage.backend.dto.response.users.ActivityLogResponse;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogExportService {

    private final ActivityLogQueryService activityLogQueryService;


    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );


    // =========================================================
    // EXPORT CSV
    // =========================================================

    public byte[] exportCsv(

            String keyword,

            ActivityAction action,

            ActivityEntityType entityType,

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        List<ActivityLogResponse> logs =
                activityLogQueryService
                        .getActivityLogsForExport(
                                keyword,
                                action,
                                entityType,
                                fromDate,
                                toDate
                        );


        StringBuilder csv =
                new StringBuilder();


        // =====================================================
        // UTF-8 BOM
        //
        // Giúp Excel hiển thị tiếng Việt đúng.
        // =====================================================

        csv.append(
                '\uFEFF'
        );


        // =====================================================
        // HEADER
        // =====================================================

        csv.append(
                "ID,"
        );

        csv.append(
                "ActivityLogCode,"
        );

        csv.append(
                "ActorUserId,"
        );

        csv.append(
                "ActorUserCode,"
        );

        csv.append(
                "ActorName,"
        );

        csv.append(
                "ActorEmail,"
        );

        csv.append(
                "ActorRoles,"
        );

        csv.append(
                "Action,"
        );

        csv.append(
                "EntityType,"
        );

        csv.append(
                "EntityId,"
        );

        csv.append(
                "OldValue,"
        );

        csv.append(
                "NewValue,"
        );

        csv.append(
                "IPAddress,"
        );

        csv.append(
                "DeviceInfo,"
        );

        csv.append(
                "CreatedAt"
        );

        csv.append(
                "\r\n"
        );


        // =====================================================
        // DATA
        // =====================================================

        for (
                ActivityLogResponse log
                : logs
        ) {

            appendColumn(
                    csv,
                    valueOf(
                            log.getId()
                    )
            );

            appendColumn(
                    csv,
                    log.getActivityLogCode()
            );

            appendColumn(
                    csv,
                    valueOf(
                            log.getActorUserId()
                    )
            );

            appendColumn(
                    csv,
                    log.getActorUserCode()
            );

            appendColumn(
                    csv,
                    log.getActorName()
            );

            appendColumn(
                    csv,
                    log.getActorEmail()
            );

            appendColumn(
                    csv,
                    joinRoles(
                            log.getActorRoles()
                    )
            );

            appendColumn(
                    csv,
                    log.getAction()
            );

            appendColumn(
                    csv,
                    log.getEntityType()
            );

            appendColumn(
                    csv,
                    valueOf(
                            log.getEntityId()
                    )
            );

            appendColumn(
                    csv,
                    log.getOldValue()
            );

            appendColumn(
                    csv,
                    log.getNewValue()
            );

            appendColumn(
                    csv,
                    log.getIpAddress()
            );

            appendColumn(
                    csv,
                    log.getDeviceInfo()
            );


            csv.append(
                    escapeCsv(
                            formatDateTime(
                                    log.getCreatedAt()
                            )
                    )
            );

            csv.append(
                    "\r\n"
            );
        }


        return csv
                .toString()
                .getBytes(
                        StandardCharsets.UTF_8
                );
    }


    // =========================================================
    // APPEND CSV COLUMN
    // =========================================================

    private void appendColumn(

            StringBuilder csv,

            String value
    ) {

        csv.append(
                escapeCsv(
                        value
                )
        );

        csv.append(
                ","
        );
    }


    // =========================================================
    // ESCAPE CSV
    //
    // Example:
    //
    // Hello "Admin"
    //
    // becomes:
    //
    // "Hello ""Admin"""
    // =========================================================

    private String escapeCsv(
            String value
    ) {

        if (value == null) {

            return "\"\"";
        }


        String escaped =
                value.replace(
                        "\"",
                        "\"\""
                );


        return "\""
                + escaped
                + "\"";
    }


    // =========================================================
    // ROLES
    // =========================================================

    private String joinRoles(
            List<String> roles
    ) {

        if (
                roles == null
                        || roles.isEmpty()
        ) {

            return "";
        }


        return String.join(
                " | ",
                roles
        );
    }


    // =========================================================
    // LONG -> STRING
    // =========================================================

    private String valueOf(
            Long value
    ) {

        if (value == null) {

            return "";
        }


        return String.valueOf(
                value
        );
    }


    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatDateTime(
            LocalDateTime value
    ) {

        if (value == null) {

            return "";
        }


        return value.format(
                DATE_TIME_FORMATTER
        );
    }
}