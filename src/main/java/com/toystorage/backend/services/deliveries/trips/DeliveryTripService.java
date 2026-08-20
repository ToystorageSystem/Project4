package com.toystorage.backend.services.deliveries.trips;

import com.toystorage.backend.dto.request.deliveries.trips
        .RejectDeliveryTripRequest;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripActionResponse;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripDetailResponse;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripListResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;

import com.toystorage.backend.entity.deliveries
        .DeliveryAssignmentHistory;

import com.toystorage.backend.entity.shipments
        .ShipmentManifestPackage;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.deliveries
        .DeliveryStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.deliveries.trips
        .DeliveryTripMapper;

import com.toystorage.backend.repository.deliveries
        .DeliveryAssignmentHistoryRepository;

import com.toystorage.backend.repository.deliveries
        .DeliveryRepository;

import com.toystorage.backend.repository.shipments
        .ShipmentManifestPackageRepository;

import com.toystorage.backend.services.deliveries.assignment
        .DeliveryAssignmentService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryTripService {

    private final DeliveryRepository
            deliveryRepository;

    private final DeliveryAssignmentHistoryRepository
            assignmentHistoryRepository;

    private final ShipmentManifestPackageRepository
            manifestPackageRepository;

    private final DeliveryTripValidationService
            validationService;

    private final DeliveryAssignmentService
            assignmentService;

    private final DeliveryTripMapper
            mapper;


    // =====================================================
    // MY TRIPS
    // =====================================================

    @Transactional(readOnly = true)
    public List<DeliveryTripListResponse>
    getMyDeliveries() {

        Users driver =
                validationService
                        .getCurrentUser();


        List<Deliveries> deliveries =
                deliveryRepository
                        .findByDriverIdAndDeliveryStatusInOrderByCreatedAtDesc(
                                driver.getId(),

                                List.of(
                                        DeliveryStatus.ASSIGNED,
                                        DeliveryStatus.ACCEPTED,
                                        DeliveryStatus.READY_TO_SHIP,
                                        DeliveryStatus.IN_TRANSIT,
                                        DeliveryStatus.ARRIVED
                                )
                        );


        return deliveries.stream()

                .map(delivery ->
                        mapper.toListResponse(
                                delivery,
                                getManifestPackages(
                                        delivery
                                ).size()
                        )
                )

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public DeliveryTripDetailResponse getDetail(
            Long deliveryId
    ) {

        Users driver =
                validationService
                        .getCurrentUser();


        Deliveries delivery =
                validationService
                        .getMyDelivery(
                                deliveryId,
                                driver
                        );


        return mapper.toDetailResponse(
                delivery,
                getManifestPackages(
                        delivery
                )
        );
    }


    // =====================================================
    // ACCEPT
    // =====================================================

    @Transactional
    public DeliveryTripDetailResponse accept(
            Long deliveryId
    ) {

        Users driver =
                validationService
                        .getCurrentUser();


        Deliveries delivery =
                validationService
                        .getMyDelivery(
                                deliveryId,
                                driver
                        );


        if (delivery.getDeliveryStatus()
                != DeliveryStatus.ASSIGNED) {

            throw new BadRequest(
                    "Only ASSIGNED delivery can be accepted"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        int updated =
                deliveryRepository
                        .acceptAssignedDelivery(

                                deliveryId,

                                driver.getId(),

                                DeliveryStatus.ASSIGNED,

                                DeliveryStatus.ACCEPTED,

                                now
                        );


        if (updated == 0) {

            throw new BadRequest(
                    "Delivery has already changed status"
            );
        }


        DeliveryAssignmentHistory history =
                assignmentHistoryRepository
                        .findTopByDeliveryIdAndDriverIdOrderByAssignedAtDesc(
                                deliveryId,
                                driver.getId()
                        )

                        .orElseThrow(() ->
                                new BadRequest(
                                        "Delivery assignment history not found"
                                )
                        );


        history.setAcceptedAt(
                now
        );


        assignmentHistoryRepository.save(
                history
        );


        delivery =
                validationService
                        .getMyDelivery(
                                deliveryId,
                                driver
                        );


        return mapper.toDetailResponse(
                delivery,
                getManifestPackages(
                        delivery
                )
        );
    }


    // =====================================================
    // REJECT
    // =====================================================

    @Transactional
    public DeliveryTripActionResponse reject(
            Long deliveryId,
            RejectDeliveryTripRequest request
    ) {

        Users driver =
                validationService
                        .getCurrentUser();


        Deliveries delivery =
                validationService
                        .getMyDelivery(
                                deliveryId,
                                driver
                        );


        if (delivery.getDeliveryStatus()
                != DeliveryStatus.ASSIGNED) {

            throw new BadRequest(
                    "Only ASSIGNED delivery can be rejected"
            );
        }


        String reason =
                request.getReason() == null
                        ? null
                        : request
                        .getReason()
                        .trim();


        if (reason == null
                || reason.isBlank()) {

            throw new BadRequest(
                    "Rejection reason is required"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        // =====================================================
        // REJECT CURRENT DRIVER
        // =====================================================

        int updated =
                deliveryRepository
                        .rejectAssignedDelivery(

                                deliveryId,

                                driver.getId(),

                                reason,

                                DeliveryStatus.ASSIGNED,

                                DeliveryStatus.REJECTED,

                                now
                        );


        if (updated == 0) {

            throw new BadRequest(
                    "Delivery has already changed status"
            );
        }


        // =====================================================
        // UPDATE HISTORY
        // =====================================================

        DeliveryAssignmentHistory history =
                assignmentHistoryRepository
                        .findTopByDeliveryIdAndDriverIdOrderByAssignedAtDesc(
                                deliveryId,
                                driver.getId()
                        )

                        .orElseThrow(() ->
                                new BadRequest(
                                        "Delivery assignment history not found"
                                )
                        );


        history.setRejectedAt(
                now
        );

        history.setRejectionReason(
                reason
        );


        assignmentHistoryRepository.save(
                history
        );


        // =====================================================
        // TRY REASSIGN
        //
        // Nếu không còn tài xế:
        // Delivery sẽ ở CREATED,
        // KHÔNG rollback reject.
        // =====================================================

        var assignment =
                assignmentService
                        .reassign(
                                deliveryId
                        );


        boolean reassigned =
                assignment.getDriverId()
                        != null;


        return DeliveryTripActionResponse
                .builder()

                .deliveryId(
                        deliveryId
                )

                .status(
                        reassigned
                                ? DeliveryStatus.ASSIGNED.name()
                                : DeliveryStatus.CREATED.name()
                )

                .message(
                        reassigned

                                ? "Delivery rejected successfully "
                                + "and reassigned to another driver"

                                : "Delivery rejected successfully. "
                                + "No other driver is currently available"
                )

                .build();
    }


    // =====================================================
    // MANIFEST PACKAGES
    // =====================================================

    private List<ShipmentManifestPackage>
    getManifestPackages(
            Deliveries delivery
    ) {

        if (delivery.getManifest() == null) {

            return List.of();
        }


        return manifestPackageRepository
                .findByManifestId(
                        delivery
                                .getManifest()
                                .getId()
                );
    }
}