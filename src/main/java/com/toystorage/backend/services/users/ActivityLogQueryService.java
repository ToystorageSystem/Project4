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
import java.util.List;

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

        validateDateRange(
                fromDate,
                toDate
        );


        Specification<ActivityLogs> specification =
                buildSpecification(
                        keyword,
                        action,
                        entityType,
                        fromDate,
                        toDate
                );


        return activityLogRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }


    // =========================================================
    // EXPORT
    //
    // Lấy toàn bộ Activity Log phù hợp filter.
    // Không phân trang.
    //
    // Dùng cho:
    //
    // GET /api/admin/activity-logs/export
    // =========================================================

    public List<ActivityLogResponse> getActivityLogsForExport(

            String keyword,

            ActivityAction action,

            ActivityEntityType entityType,

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        Specification<ActivityLogs> specification =
                buildSpecification(
                        keyword,
                        action,
                        entityType,
                        fromDate,
                        toDate
                );


        Sort sort =
                Sort.by(

                        Sort.Order.desc(
                                "createdAt"
                        ),

                        Sort.Order.desc(
                                "id"
                        )
                );


        return activityLogRepository
                .findAll(
                        specification,
                        sort
                )
                .stream()

                .map(
                        this::toResponse
                )

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
                        .findById(
                                logId
                        )
                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Không tìm thấy nhật ký hoạt động"
                                        )
                        );


        return toResponse(
                log
        );
    }


    // =========================================================
    // USER ACTIVITY HISTORY
    //
    // Dùng cho Task #27:
    //
    // Xem toàn bộ lịch sử hoạt động
    // của User liên quan đến cảnh báo.
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
                .findByUser_IdOrderByCreatedAtDesc(
                        userId
                )
                .stream()

                .map(
                        this::toResponse
                )

                .toList();
    }


    // =========================================================
    // BUILD SPECIFICATION
    //
    // Dùng chung cho:
    //
    // - list
    // - pagination
    // - search
    // - filter
    // - export
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
            // Search theo:
            //
            // - userCode
            // - name
            // - email
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
                // USER CODE
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
                // USER NAME
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
                // EMAIL
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
            // FILTER BY ENTITY TYPE / FUNCTION
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

                        criteriaBuilder
                                .greaterThanOrEqualTo(
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

                        criteriaBuilder
                                .lessThanOrEqualTo(
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
    // =========================================================

    private ActivityLogResponse toResponse(
            ActivityLogs log
    ) {

        Users actor =
                log.getUser();


        List<String> actorRoles =
                getActorRoles(
                        actor
                );


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
    // =========================================================

    private List<String> getActorRoles(
            Users actor
    ) {

        if (actor == null) {

            return List.of();
        }


        return userRoleRepository
                .findByUser_Id(
                        actor.getId()
                )
                .stream()

                .map(
                        UserRoles::getRole
                )

                .filter(
                        role -> role != null
                )

                .map(
                        role -> role.getRoleCode()
                )

                .distinct()

                .sorted()

                .toList();
    }
}