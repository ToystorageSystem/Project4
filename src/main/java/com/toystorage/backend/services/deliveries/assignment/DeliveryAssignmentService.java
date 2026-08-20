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
     * Kiểm tra lại roles_code thực tế trong DB.
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
     * private final AiDriverScoringService
     *         aiDriverScoringService;
     */


    // =====================================================
    // AUTO ASSIGN - BẮT BUỘC CÓ DRIVER
    // =====================================================

    @Transactional
    public DeliveryAssignmentResponse autoAssign(
            Long deliveryId
    ) {

        Deliveries delivery =
                getDelivery(
                        deliveryId
                );


        /*
         * Nếu đã assign hợp lệ rồi
         * thì không assign lại.
         */
        if (delivery.getDriver() != null
                && delivery.getDeliveryStatus()
                == DeliveryStatus.ASSIGNED) {

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
    // Dùng khi Reject.
    //
    // Không có tài xế cũng KHÔNG throw.
    // =====================================================

    @Transactional
    public DeliveryAssignmentResponse tryAutoAssign(
            Long deliveryId
    ) {

        Deliveries delivery =
                getDelivery(
                        deliveryId
                );


        Optional<Users> selectedDriver =
                findBestDriver(
                        delivery
                );


        /*
         * Chưa có ai rảnh.
         *
         * Giữ CREATED + driver null.
         * Sau này scheduler/API có thể thử lại.
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
         * Bỏ owner cũ.
         */
        delivery.setDriver(
                null
        );

        delivery.setDeliveryStatus(
                DeliveryStatus.CREATED
        );

        delivery.setAcceptedAt(
                null
        );

        deliveryRepository.save(
                delivery
        );


        /*
         * Không bắt buộc phải có người ngay.
         */
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


        /*
         * Tài xế đã reject chuyến này
         * thì không assign lại.
         */
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
        // RULE BASED
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
                 * score < 0 nghĩa là không available.
                 */
                .filter(driverScore ->
                        driverScore.score() >= 0
                )

                .max(
                        Comparator
                                .comparingDouble(
                                        DriverScore::score
                                )

                                /*
                                 * Nếu bằng score:
                                 * ưu tiên ID nhỏ hơn.
                                 */
                                .thenComparing(
                                        driverScore ->
                                                -driverScore
                                                        .driver()
                                                        .getId()
                                )
                )

                .map(
                        DriverScore::driver
                );


        /*
         * =================================================
         * AI VERSION SAU NÀY
         * =================================================
         *
         * return Optional.ofNullable(
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
    // GET DELIVERY
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


    private record DriverScore(
            Users driver,
            double score
    ) {
    }
}