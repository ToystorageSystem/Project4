package com.toystorage.backend.services.deliveries.assignment;

import com.toystorage.backend.dto.response.deliveries.assignment
        .DeliveryAssignmentResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries
        .DeliveryAssignmentHistory;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.deliveries.assignment
        .DeliveryAssignmentMapper;

import com.toystorage.backend.repository.deliveries
        .DeliveryAssignmentHistoryRepository;

import com.toystorage.backend.repository.deliveries
        .DeliveryRepository;

import com.toystorage.backend.repository.users
        .UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryAssignmentService {

    /*
     * Phải đúng với roles_code trong database.
     */
    private static final String
            DELIVERY_STAFF_ROLE_CODE =
            "DELIVERY_STAFF";


    private final UserRepository
            userRepository;

    private final DeliveryRepository
            deliveryRepository;

    private final DeliveryAssignmentHistoryRepository
            assignmentHistoryRepository;

    private final DriverScoringService
            driverScoringService;

    private final DeliveryAssignmentMapper
            mapper;


    /*
     * =====================================================
     * FUTURE AI
     * =====================================================
     *
     * Sau này khi bật AI:
     *
     * private final AiDriverScoringService
     *         aiDriverScoringService;
     */


    // =====================================================
    // AUTO ASSIGN
    // =====================================================

    @Transactional
    public DeliveryAssignmentResponse autoAssign(
            Long deliveryId
    ) {

        /*
         * findByIdForUpdate()
         * lock row Delivery để tránh 2 thread
         * cùng assign 2 tài xế khác nhau.
         */
        Deliveries delivery =
                getDelivery(
                        deliveryId
                );


        // =================================================
        // ĐÃ CÓ DRIVER VÀ ĐANG ĐƯỢC XỬ LÝ
        // =================================================

        if (delivery.getDriver() != null
                && isActiveAssignmentStatus(
                delivery.getDeliveryStatus()
        )) {

            DeliveryAssignmentHistory history =
                    assignmentHistoryRepository
                            .findTopByDeliveryIdAndDriverIdOrderByAssignedAtDesc(
                                    deliveryId,
                                    delivery
                                            .getDriver()
                                            .getId()
                            )
                            .orElse(null);


            return mapper.toResponse(
                    delivery,
                    history
            );
        }


        // =================================================
        // CHỈ CREATED / REJECTED ĐƯỢC ASSIGN
        // =================================================

        if (delivery.getDeliveryStatus()
                != DeliveryStatus.CREATED
                && delivery.getDeliveryStatus()
                != DeliveryStatus.REJECTED) {

            throw new BadRequest(
                    "Delivery cannot be assigned from status "
                            + delivery.getDeliveryStatus()
            );
        }


        Users selectedDriver =
                findBestDriver(
                        delivery
                )

                        .orElseThrow(() ->
                                new BadRequest(
                                        "No Delivery Staff available"
                                )
                        );


        return assign(
                delivery,
                selectedDriver
        );
    }


    // =====================================================
    // TRY AUTO ASSIGN
    //
    // Không có tài xế thì KHÔNG throw.
    // =====================================================

    @Transactional
    public DeliveryAssignmentResponse tryAutoAssign(
            Long deliveryId
    ) {

        Deliveries delivery =
                getDelivery(
                        deliveryId
                );


        // =================================================
        // ĐÃ ASSIGN RỒI
        // =================================================

        if (delivery.getDriver() != null
                && isActiveAssignmentStatus(
                delivery.getDeliveryStatus()
        )) {

            DeliveryAssignmentHistory history =
                    assignmentHistoryRepository
                            .findTopByDeliveryIdAndDriverIdOrderByAssignedAtDesc(
                                    deliveryId,
                                    delivery
                                            .getDriver()
                                            .getId()
                            )
                            .orElse(null);


            return mapper.toResponse(
                    delivery,
                    history
            );
        }


        // =================================================
        // STATUS VALIDATION
        // =================================================

        if (delivery.getDeliveryStatus()
                != DeliveryStatus.CREATED
                && delivery.getDeliveryStatus()
                != DeliveryStatus.REJECTED) {

            throw new BadRequest(
                    "Delivery cannot be assigned from status "
                            + delivery.getDeliveryStatus()
            );
        }


        Optional<Users> selectedDriver =
                findBestDriver(
                        delivery
                );


        /*
         * Không có tài xế hiện tại.
         *
         * Để CREATED chờ hệ thống thử lại.
         */
        if (selectedDriver.isEmpty()) {

            delivery.setDriver(
                    null
            );

            delivery.setDeliveryStatus(
                    DeliveryStatus.CREATED
            );


            deliveryRepository.save(
                    delivery
            );


            return mapper.toResponse(
                    delivery,
                    null
            );
        }


        return assign(
                delivery,
                selectedDriver.get()
        );
    }


    // =====================================================
    // REASSIGN
    // =====================================================

    @Transactional
    public DeliveryAssignmentResponse reassign(
            Long deliveryId
    ) {

        Deliveries delivery =
                getDelivery(
                        deliveryId
                );


        /*
         * Không được reassign chuyến:
         *
         * ACCEPTED
         * IN_TRANSIT
         * ARRIVED
         * ...
         *
         * tránh đổi tài xế giữa chuyến.
         */
        if (delivery.getDeliveryStatus()
                != DeliveryStatus.REJECTED
                && delivery.getDeliveryStatus()
                != DeliveryStatus.CREATED) {

            throw new BadRequest(
                    "Only CREATED or REJECTED delivery "
                            + "can be reassigned"
            );
        }


        // =================================================
        // CLEAR CURRENT DRIVER
        // =================================================

        delivery.setDriver(
                null
        );

        delivery.setDeliveryStatus(
                DeliveryStatus.CREATED
        );

        delivery.setAcceptedAt(
                null
        );


        /*
         * Không xóa rejection history.
         *
         * History table vẫn giữ người đã reject.
         */

        deliveryRepository.save(
                delivery
        );


        // =================================================
        // TRY NEXT DRIVER
        // =================================================

        return tryAutoAssign(
                deliveryId
        );
    }


    // =====================================================
    // FIND BEST DRIVER
    // =====================================================

    private Optional<Users> findBestDriver(
            Deliveries delivery
    ) {

        List<Users> drivers =
                userRepository
                        .findActiveUsersByRoleCode(
                                DELIVERY_STAFF_ROLE_CODE
                        );


        if (drivers.isEmpty()) {

            return Optional.empty();
        }


        // =================================================
        // REMOVE PREVIOUS REJECTED DRIVERS
        // =================================================

        List<Users> candidates =
                drivers.stream()

                        .filter(driver ->
                                !assignmentHistoryRepository
                                        .existsByDeliveryIdAndDriverIdAndRejectedAtIsNotNull(
                                                delivery.getId(),
                                                driver.getId()
                                        )
                        )

                        .toList();


        if (candidates.isEmpty()) {

            return Optional.empty();
        }


        // =================================================
        // RULE BASED SCORING
        // =================================================

        return candidates.stream()

                .map(driver ->
                        new DriverScore(
                                driver,

                                driverScoringService
                                        .calculateScore(
                                                driver,
                                                delivery
                                        )
                        )
                )

                /*
                 * score < 0:
                 * tài xế không available.
                 */
                .filter(driverScore ->
                        driverScore.score() >= 0
                )

                /*
                 * Score cao nhất trước.
                 *
                 * Nếu cùng score:
                 * ưu tiên User ID nhỏ hơn
                 * để kết quả deterministic.
                 */
                .sorted(
                        Comparator
                                .comparingDouble(
                                        DriverScore::score
                                )
                                .reversed()

                                .thenComparing(
                                        driverScore ->
                                                driverScore
                                                        .driver()
                                                        .getId()
                                )
                )

                .map(
                        DriverScore::driver
                )

                .findFirst();


        /*
         * =================================================
         * FUTURE AI VERSION
         * =================================================
         *
         * return Optional.ofNullable(
         *
         *         aiDriverScoringService
         *                 .selectBestDriver(
         *                         candidates,
         *                         delivery
         *                 )
         * );
         */
    }


    // =====================================================
    // ASSIGN
    // =====================================================

    private DeliveryAssignmentResponse assign(
            Deliveries delivery,
            Users selectedDriver
    ) {

        LocalDateTime now =
                LocalDateTime.now();


        // =================================================
        // DELIVERY
        // =================================================

        delivery.setDriver(
                selectedDriver
        );

        delivery.setDeliveryStatus(
                DeliveryStatus.ASSIGNED
        );

        delivery.setAcceptedAt(
                null
        );

        delivery.setRejectedAt(
                null
        );

        delivery.setRejectionReason(
                null
        );


        delivery =
                deliveryRepository.save(
                        delivery
                );


        // =================================================
        // ASSIGNMENT HISTORY
        // =================================================

        DeliveryAssignmentHistory history =
                DeliveryAssignmentHistory
                        .builder()

                        .delivery(
                                delivery
                        )

                        .driver(
                                selectedDriver
                        )

                        .assignedAt(
                                now
                        )

                        .build();


        history =
                assignmentHistoryRepository
                        .save(
                                history
                        );


        return mapper.toResponse(
                delivery,
                history
        );
    }


    // =====================================================
    // ACTIVE ASSIGNMENT STATUS
    // =====================================================

    private boolean isActiveAssignmentStatus(
            DeliveryStatus status
    ) {

        if (status == null) {
            return false;
        }


        return status
                == DeliveryStatus.ASSIGNED

                || status
                == DeliveryStatus.ACCEPTED

                || status
                == DeliveryStatus.READY_TO_SHIP

                || status
                == DeliveryStatus.IN_TRANSIT

                || status
                == DeliveryStatus.ARRIVED;
    }


    // =====================================================
    // GET DELIVERY WITH LOCK
    // =====================================================

    private Deliveries getDelivery(
            Long deliveryId
    ) {

        return deliveryRepository
                .findByIdForUpdate(
                        deliveryId
                )

                .orElseThrow(() ->
                        new BadRequest(
                                "Delivery not found: "
                                        + deliveryId
                        )
                );
    }


    // =====================================================
    // DRIVER SCORE
    // =====================================================

    private record DriverScore(
            Users driver,
            double score
    ) {
    }
}