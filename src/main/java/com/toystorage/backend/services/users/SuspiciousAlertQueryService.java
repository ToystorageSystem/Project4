package com.toystorage.backend.services.users;

import com.toystorage.backend.dto.response.users.SuspiciousAlertResponse;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.SuspiciousActivityAlerts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.SuspiciousAlertLevel;
import com.toystorage.backend.enums.users.SuspiciousAlertStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.users.SuspiciousActivityAlertRepository;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuspiciousAlertQueryService {

    private final SuspiciousActivityAlertRepository
            suspiciousActivityAlertRepository;


    // =========================================================
    // LIST + SEARCH + FILTER + PAGINATION
    // =========================================================

    public Page<SuspiciousAlertResponse> getAlerts(

            String keyword,

            SuspiciousAlertLevel alertLevel,

            SuspiciousAlertStatus status,

            LocalDateTime fromDate,

            LocalDateTime toDate,

            Pageable pageable
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        Specification<SuspiciousActivityAlerts> specification =
                buildSpecification(
                        keyword,
                        alertLevel,
                        status,
                        fromDate,
                        toDate
                );


        return suspiciousActivityAlertRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }


    // =========================================================
    // DETAIL
    // =========================================================

    public SuspiciousAlertResponse getAlertById(
            Long alertId
    ) {

        SuspiciousActivityAlerts alert =
                suspiciousActivityAlertRepository
                        .findById(
                                alertId
                        )
                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Không tìm thấy cảnh báo hoạt động bất thường"
                                        )
                        );


        return toResponse(
                alert
        );
    }


    // =========================================================
    // GET ENTITY
    //
    // Dùng lại cho Management Service ở bước sau.
    // =========================================================

    public SuspiciousActivityAlerts getAlertEntity(
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
    // BUILD SPECIFICATION
    // =========================================================

    private Specification<SuspiciousActivityAlerts>
    buildSpecification(

            String keyword,

            SuspiciousAlertLevel alertLevel,

            SuspiciousAlertStatus status,

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
            // SEARCH
            //
            // Theo:
            //
            // - alert code
            // - user code
            // - user name
            // - user email
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


                Predicate alertCodePredicate =
                        criteriaBuilder.like(

                                criteriaBuilder.lower(
                                        root.get(
                                                "suspiciousActivityAlertCode"
                                        )
                                ),

                                searchValue
                        );


                Predicate userCodePredicate =
                        criteriaBuilder.like(

                                criteriaBuilder.lower(
                                        root
                                                .get("user")
                                                .get("userCode")
                                ),

                                searchValue
                        );


                Predicate userNamePredicate =
                        criteriaBuilder.like(

                                criteriaBuilder.lower(
                                        root
                                                .get("user")
                                                .get("name")
                                ),

                                searchValue
                        );


                Predicate userEmailPredicate =
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
                                alertCodePredicate,
                                userCodePredicate,
                                userNamePredicate,
                                userEmailPredicate
                        )
                );
            }


            // =====================================================
            // FILTER ALERT LEVEL
            // =====================================================

            if (alertLevel != null) {

                predicates.add(

                        criteriaBuilder.equal(
                                root.get("alertLevel"),
                                alertLevel
                        )
                );
            }


            // =====================================================
            // FILTER STATUS
            // =====================================================

            if (status != null) {

                predicates.add(

                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }


            // =====================================================
            // FILTER FROM DETECTED DATE
            // =====================================================

            if (fromDate != null) {

                predicates.add(

                        criteriaBuilder
                                .greaterThanOrEqualTo(
                                        root.get("detectedAt"),
                                        fromDate
                                )
                );
            }


            // =====================================================
            // FILTER TO DETECTED DATE
            // =====================================================

            if (toDate != null) {

                predicates.add(

                        criteriaBuilder
                                .lessThanOrEqualTo(
                                        root.get("detectedAt"),
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

    public SuspiciousAlertResponse toResponse(
            SuspiciousActivityAlerts alert
    ) {

        Users user =
                alert.getUser();


        Users reviewer =
                alert.getReviewedBy();


        ActivityLogs relatedLog =
                alert.getRelatedLog();


        return SuspiciousAlertResponse
                .builder()


                // =================================================
                // ALERT
                // =================================================

                .id(
                        alert.getId()
                )

                .alertCode(
                        alert.getSuspiciousActivityAlertCode()
                )

                .riskScore(
                        alert.getRiskScore()
                )

                .alertLevel(
                        alert.getAlertLevel()
                )

                .status(
                        alert.getStatus()
                )

                .description(
                        alert.getDescription()
                )


                // =================================================
                // RELATED USER
                // =================================================

                .userId(
                        user != null
                                ? user.getId()
                                : null
                )

                .userCode(
                        user != null
                                ? user.getUserCode()
                                : null
                )

                .userName(
                        user != null
                                ? user.getName()
                                : null
                )

                .userEmail(
                        user != null
                                ? user.getEmail()
                                : null
                )


                // =================================================
                // RELATED ACTIVITY LOG
                // =================================================

                .relatedLogId(
                        relatedLog != null
                                ? relatedLog.getId()
                                : null
                )

                .relatedLogCode(
                        relatedLog != null
                                ? relatedLog.getActivityLogsCode()
                                : null
                )

                .relatedLogAction(
                        relatedLog != null
                                && relatedLog.getAction() != null

                                ? relatedLog
                                .getAction()
                                .name()

                                : null
                )

                .relatedLogEntityType(
                        relatedLog != null
                                && relatedLog.getEntityType() != null

                                ? relatedLog
                                .getEntityType()
                                .name()

                                : null
                )

                .relatedLogCreatedAt(
                        relatedLog != null
                                ? relatedLog.getCreatedAt()
                                : null
                )


                // =================================================
                // REVIEWER
                // =================================================

                .reviewedById(
                        reviewer != null
                                ? reviewer.getId()
                                : null
                )

                .reviewedByUserCode(
                        reviewer != null
                                ? reviewer.getUserCode()
                                : null
                )

                .reviewedByName(
                        reviewer != null
                                ? reviewer.getName()
                                : null
                )

                .reviewedByEmail(
                        reviewer != null
                                ? reviewer.getEmail()
                                : null
                )


                // =================================================
                // PROCESSING
                // =================================================

                .resolutionNote(
                        alert.getResolutionNote()
                )

                .detectedAt(
                        alert.getDetectedAt()
                )

                .reviewedAt(
                        alert.getReviewedAt()
                )

                .resolvedAt(
                        alert.getResolvedAt()
                )

                .updatedAt(
                        alert.getUpdatedAt()
                )


                .build();
    }
}