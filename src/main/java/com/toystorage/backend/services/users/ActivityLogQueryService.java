
package com.toystorage.backend.services.users;

import com.toystorage.backend.dto.response.users.ActivityLogResponse;

import com.toystorage.backend.entity.auth.UserRoles;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogQueryService {

    private final ActivityLogRepository activityLogRepository;

    private final UserRoleRepository userRoleRepository;

    // =========================================================
    // LIST + SEARCH + FILTER + PAGINATION
    // =========================================================

    public Page<ActivityLogResponse> getActivityLogs(
            String keyword,
            ActivityAction action,
            ActivityEntityType entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {

        validateDateRange(fromDate, toDate);

        Specification<ActivityLogs> specification =
                buildSpecification(
                        keyword,
                        action,
                        entityType,
                        fromDate,
                        toDate
                );

        return activityLogRepository
                .findAll(specification, pageable)
                .map(this::toResponse);
    }

    // =========================================================
    // EXPORT ACTIVITY LOGS
    //
    // Lay toan bo nhat ky phu hop voi bo loc.
    // Khong phan trang.
    //
    // Toi uu:
    // Cache role theo User ID trong mot lan export.
    // Tranh truy van role lap lai cho cung mot user.
    //
    // API:
    // GET /api/admin/activity-logs/export
    // =========================================================

    public List<ActivityLogResponse> getActivityLogsForExport(
            String keyword,
            ActivityAction action,
            ActivityEntityType entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        // Kiem tra khoang thoi gian
        validateDateRange(fromDate, toDate);

        // Tao dieu kien tim kiem va loc
        Specification<ActivityLogs> specification =
                buildSpecification(
                        keyword,
                        action,
                        entityType,
                        fromDate,
                        toDate
                );

        // Sap xep nhat ky moi nhat truoc
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        /*
         * Cache role cua user trong pham vi mot lan export.
         *
         * Key: User ID
         * Value: Danh sach role code cua user.
         *
         * Vi du:
         * User 10 -> [ROLE-ADMIN]
         *
         * Neu 100 nhat ky do cung User 10 tao ra,
         * Backend chi can truy van role cua User 10
         * mot lan thay vi 100 lan.
         */
        Map<Long, List<String>> roleCache =
                new HashMap<>();

        // Lay tat ca nhat ky phu hop bo loc
        return activityLogRepository
                .findAll(specification, sort)
                .stream()
                .map(log -> {

                    // Lay nguoi thuc hien
                    Users actor = log.getUser();

                    List<String> actorRoles;

                    if (actor == null) {

                        // Nhat ky khong co user
                        actorRoles = List.of();

                    } else {

                        Long actorId = actor.getId();

                        /*
                         * Neu cache da co role cua user:
                         * Su dung lai ket qua.
                         *
                         * Neu chua co:
                         * Truy van database va luu vao cache.
                         */
                        actorRoles =
                                roleCache.computeIfAbsent(
                                        actorId,
                                        id -> getActorRoles(actor)
                                );
                    }

                    // Map entity sang DTO voi role da cache
                    return toResponse(
                            log,
                            actorRoles
                    );
                })
                .toList();
    }

    // =========================================================
    // DETAIL
    // =========================================================

    public ActivityLogResponse getActivityLogById(
            Long logId
    ) {

        ActivityLogs log =
                activityLogRepository
                        .findById(logId)
                        .orElseThrow(
                                () -> new NotFound(
                                        "Không tìm thấy nhật ký hoạt động"
                                )
                        );

        return toResponse(log);
    }

    // =========================================================
    // USER ACTIVITY HISTORY
    //
    // Dung cho Task #27:
    // Xem lich su hoat dong cua user lien quan canh bao.
    // =========================================================

    public List<ActivityLogResponse> getUserActivityHistory(
            Long userId
    ) {

        if (userId == null) {

            throw new BadRequest(
                    "ID người dùng không được để trống"
            );
        }

        return activityLogRepository
                .findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // BUILD SPECIFICATION
    //
    // Dung chung cho:
    // - List
    // - Pagination
    // - Search
    // - Filter
    // - Export
    // =========================================================

    private Specification<ActivityLogs> buildSpecification(
            String keyword,
            ActivityAction action,
            ActivityEntityType entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        return (
                root,
                query,
                criteriaBuilder
        ) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            // =====================================================
            // SEARCH ACTOR
            //
            // Tim theo:
            // - User Code
            // - User Name
            // - Email
            // =====================================================

            if (
                    keyword != null
                            && !keyword.trim().isEmpty()
            ) {

                String searchValue =
                        "%"
                                + keyword
                                .trim()
                                .toLowerCase()
                                + "%";

                // -------------------------------------------------
                // SEARCH USER CODE
                // -------------------------------------------------

                Predicate userCodePredicate =
                        criteriaBuilder.like(

                                criteriaBuilder.lower(
                                        root
                                                .get("user")
                                                .get("userCode")
                                ),

                                searchValue
                        );

                // -------------------------------------------------
                // SEARCH USER NAME
                // -------------------------------------------------

                Predicate userNamePredicate =
                        criteriaBuilder.like(

                                criteriaBuilder.lower(
                                        root
                                                .get("user")
                                                .get("name")
                                ),

                                searchValue
                        );

                // -------------------------------------------------
                // SEARCH EMAIL
                // -------------------------------------------------

                Predicate emailPredicate =
                        criteriaBuilder.like(

                                criteriaBuilder.lower(
                                        root
                                                .get("user")
                                                .get("email")
                                ),

                                searchValue
                        );

                predicates.add(
                        criteriaBuilder.or(
                                userCodePredicate,
                                userNamePredicate,
                                emailPredicate
                        )
                );
            }

            // =====================================================
            // FILTER BY ACTION
            // =====================================================

            if (action != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("action"),
                                action
                        )
                );
            }

            // =====================================================
            // FILTER BY ENTITY TYPE
            // =====================================================

            if (entityType != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("entityType"),
                                entityType
                        )
                );
            }

            // =====================================================
            // FILTER FROM DATE
            // =====================================================

            if (fromDate != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                fromDate
                        )
                );
            }

            // =====================================================
            // FILTER TO DATE
            // =====================================================

            if (toDate != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdAt"),
                                toDate
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    // =========================================================
    // VALIDATE DATE RANGE
    // =========================================================

    private void validateDateRange(
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(toDate)
        ) {

            throw new BadRequest(
                    "Thời gian bắt đầu không được sau thời gian kết thúc"
            );
        }
    }

    // =========================================================
    // ENTITY -> RESPONSE
    //
    // Su dung cho:
    // - List
    // - Detail
    // - User Activity History
    //
    // Cac chuc nang cu van hoat dong nhu truoc.
    // =========================================================

    private ActivityLogResponse toResponse(
            ActivityLogs log
    ) {

        Users actor = log.getUser();

        List<String> actorRoles =
                getActorRoles(actor);

        return toResponse(
                log,
                actorRoles
        );
    }

    // =========================================================
    // ENTITY -> RESPONSE WITH CACHED ROLES
    //
    // Su dung cho Export CSV.
    //
    // Khong truy van lai role vi da nhan
    // danh sach actorRoles tu cache.
    // =========================================================

    private ActivityLogResponse toResponse(
            ActivityLogs log,
            List<String> actorRoles
    ) {

        Users actor = log.getUser();

        return ActivityLogResponse
                .builder()

                // =================================================
                // LOG INFORMATION
                // =================================================

                .id(
                        log.getId()
                )

                .activityLogCode(
                        log.getActivityLogsCode()
                )

                // =================================================
                // ACTOR
                // =================================================

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

                .actorRoles(
                        actorRoles
                )

                // =================================================
                // ACTION
                // =================================================

                .action(
                        log.getAction() != null
                                ? log.getAction().name()
                                : null
                )

                // =================================================
                // TARGET ENTITY
                // =================================================

                .entityType(
                        log.getEntityType() != null
                                ? log.getEntityType().name()
                                : null
                )

                .entityId(
                        log.getEntityId()
                )

                // =================================================
                // BEFORE / AFTER VALUE
                // =================================================

                .oldValue(
                        log.getOldValue()
                )

                .newValue(
                        log.getNewValue()
                )

                // =================================================
                // CLIENT INFORMATION
                // =================================================

                .ipAddress(
                        log.getIpAddress()
                )

                .deviceInfo(
                        log.getDeviceInfo()
                )

                // =================================================
                // CREATED TIME
                // =================================================

                .createdAt(
                        log.getCreatedAt()
                )

                .build();
    }

    // =========================================================
    // GET ACTOR ROLES
    //
    // Lay danh sach role cua nguoi thuc hien.
    // =========================================================

    private List<String> getActorRoles(
            Users actor
    ) {

        if (actor == null) {
            return List.of();
        }

        return userRoleRepository
                .findByUser_Id(actor.getId())
                .stream()
                .map(UserRoles::getRole)
                .filter(role -> role != null)
                .map(role -> role.getRoleCode())
                .distinct()
                .sorted()
                .toList();
    }
}
