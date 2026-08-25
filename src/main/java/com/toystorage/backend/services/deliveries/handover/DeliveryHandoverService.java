package com.toystorage.backend.services.deliveries.handover;

import com.toystorage.backend.dto.request.deliveries.handover.*;
import com.toystorage.backend.dto.response.deliveries.handover.*;

import com.toystorage.backend.entity.deliveries.*;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.deliveries.*;
import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.deliveries.handover
        .DeliveryHandoverMapper;

import com.toystorage.backend.repository.deliveries.*;
import com.toystorage.backend.repository.packages.packing.PackageRepository;
import com.toystorage.backend.repository.shipments
        .ShipmentManifestPackageRepository;
import com.toystorage.backend.repository.users.UserRepository;

import com.toystorage.backend.services.deliveries.tracking
        .DeliveryTrackingLifecycleService;
import com.toystorage.backend.services.deliveries.trips
        .DeliveryTripValidationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryHandoverService {

    private final DeliveryHandoverRepository
            handoverRepository;

    private final DeliveryHandoverItemRepository
            handoverItemRepository;

    private final ShipmentManifestPackageRepository
            manifestPackageRepository;

    private final PackageRepository
            packageRepository;

    private final DeliveryRepository
            deliveryRepository;

    private final UserRepository
            userRepository;

    private final DeliveryTripValidationService
            tripValidationService;

    private final DeliveryHandoverValidationService
            validationService;

    private final DeliveryHandoverMapper
            mapper;

    private final DeliveryTrackingLifecycleService
            trackingLifecycleService;


    // =====================================================
    // SCAN PACKAGE
    // =====================================================

    @Transactional
    public DeliveryHandoverResponse scanPackage(
            Long deliveryId,
            ScanHandoverPackageRequest request
    ) {

        Users driver =
                tripValidationService.getCurrentUser();

        Deliveries delivery =
                tripValidationService.getMyDelivery(
                        deliveryId,
                        driver
                );

        validationService.validateCanHandover(
                delivery,
                driver
        );


        DeliveryHandover handover =
                getOrCreateHandover(
                        delivery,
                        driver
                );

        validationService.validateNotCompleted(
                handover
        );


        Packages packageEntity =
                packageRepository
                        .findByPackagesCode(
                                request.getPackageCode()
                        )
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Package not found"
                                )
                        );


        // Kiểm kiện có thuộc manifest không.
        List<ShipmentManifestPackage> manifestPackages =
                getManifestPackages(delivery);

        boolean belongsToManifest =
                manifestPackages.stream()
                        .anyMatch(item ->
                                item.getPackageEntity()
                                        .getId()
                                        .equals(
                                                packageEntity.getId()
                                        )
                        );


        if (!belongsToManifest) {

            throw new BadRequest(
                    "Package does not belong to this delivery manifest"
            );
        }


        // Không bàn giao trùng.
        if (handoverItemRepository
                .existsByHandoverIdAndPackageEntityId(
                        handover.getId(),
                        packageEntity.getId()
                )) {

            throw new BadRequest(
                    "Package has already been handed over"
            );
        }


        HandoverPackageCondition condition =
                request.getCondition();


        String issueNote =
                request.getIssueNote() == null
                        ? null
                        : request.getIssueNote().trim();


        // Bất thường bắt buộc có ghi chú.
        if (condition != HandoverPackageCondition.NORMAL
                && (issueNote == null
                || issueNote.isBlank())) {

            throw new BadRequest(
                    "Issue note is required for abnormal package"
            );
        }


        DeliveryHandoverItem item =
                DeliveryHandoverItem.builder()

                        .handover(
                                handover
                        )

                        .packageEntity(
                                packageEntity
                        )

                        .actualSealNumber(
                                request.getActualSealNumber()
                        )

                        .condition(
                                condition
                        )

                        .issueNote(
                                issueNote
                        )

                        .scannedAt(
                                LocalDateTime.now()
                        )

                        .build();


        handoverItemRepository.save(
                item
        );


        return buildResponse(
                handover
        );
    }


    // =====================================================
    // COMPLETE HANDOVER
    // =====================================================

    @Transactional
    public DeliveryHandoverResponse complete(
            Long deliveryId,
            CompleteDeliveryHandoverRequest request
    ) {

        Users driver =
                tripValidationService.getCurrentUser();

        Deliveries delivery =
                tripValidationService.getMyDelivery(
                        deliveryId,
                        driver
                );

        validationService.validateCanHandover(
                delivery,
                driver
        );


        DeliveryHandover handover =
                handoverRepository
                        .findByDeliveryId(
                                deliveryId
                        )

                        .orElseThrow(() ->
                                new BadRequest(
                                        "No handover has been started"
                                )
                        );


        validationService.validateNotCompleted(
                handover
        );


        Users receiver =
                userRepository
                        .findById(
                                request.getReceiverId()
                        )

                        .orElseThrow(() ->
                                new BadRequest(
                                        "Receiver not found"
                                )
                        );


        /*
         * Người nhận bắt buộc thuộc đúng
         * warehouse/store đích.
         */
        if (receiver.getWarehouse() == null
                || delivery.getToWarehouse() == null
                || !receiver.getWarehouse()
                .getId()
                .equals(
                        delivery.getToWarehouse().getId()
                )) {

            throw new BadRequest(
                    "Receiver does not belong to destination location"
            );
        }


        List<ShipmentManifestPackage> expected =
                getManifestPackages(
                        delivery
                );

        List<DeliveryHandoverItem> actual =
                handoverItemRepository
                        .findByHandoverIdOrderByScannedAtAsc(
                                handover.getId()
                        );


        /*
         * Thiếu kiện vẫn có thể hoàn tất nếu đã được
         * ghi nhận MISSING.
         *
         * Tuy nhiên với thiết kế hiện tại MISSING cần
         * được tạo thành handover item.
         */
        if (actual.size() != expected.size()) {

            throw new BadRequest(
                    "All expected packages must be accounted for "
                            + "before completing handover"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        handover.setReceivedBy(
                receiver
        );

        handover.setNote(
                request.getNote()
        );

        handover.setCompletedAt(
                now
        );


        handoverRepository.save(
                handover
        );


        // =================================================
        // DELIVERY
        // =================================================

        delivery.setDeliveryStatus(
                DeliveryStatus.DELIVERED
        );

        delivery.setDeliveredAt(
                now
        );

        delivery.setHandedOverBy(
                driver
        );

        delivery.setHandedOverAt(
                now
        );


        deliveryRepository.save(
                delivery
        );


        trackingLifecycleService.stopTracking(
                deliveryId
        );


        return buildResponse(
                handover
        );
    }


    // =====================================================
    // GET OR CREATE
    // =====================================================

    private DeliveryHandover getOrCreateHandover(
            Deliveries delivery,
            Users driver
    ) {

        return handoverRepository
                .findByDeliveryId(
                        delivery.getId()
                )

                .orElseGet(() ->
                        handoverRepository.save(

                                DeliveryHandover.builder()

                                        .delivery(
                                                delivery
                                        )

                                        .handedOverBy(
                                                driver
                                        )

                                        .build()
                        )
                );
    }


    // =====================================================
    // MANIFEST PACKAGES
    // =====================================================

    private List<ShipmentManifestPackage>
    getManifestPackages(
            Deliveries delivery
    ) {

        if (delivery.getManifest() == null) {

            throw new BadRequest(
                    "Delivery does not have a shipment manifest"
            );
        }


        return manifestPackageRepository
                .findByManifestId(
                        delivery
                                .getManifest()
                                .getId()
                );
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private DeliveryHandoverResponse buildResponse(
            DeliveryHandover handover
    ) {

        List<ShipmentManifestPackage> expected =
                getManifestPackages(
                        handover.getDelivery()
                );


        List<DeliveryHandoverItem> actual =
                handoverItemRepository
                        .findByHandoverIdOrderByScannedAtAsc(
                                handover.getId()
                        );


        int missing =
                (int) actual.stream()
                        .filter(item ->
                                item.getCondition()
                                        == HandoverPackageCondition.MISSING
                        )
                        .count();


        int abnormal =
                (int) actual.stream()
                        .filter(item ->
                                item.getCondition()
                                        != HandoverPackageCondition.NORMAL
                        )
                        .count();


        return mapper.toResponse(
                handover,
                actual,
                expected.size(),
                missing,
                abnormal
        );
    }
}