package com.toystorage.backend.services.deliveries.tracking;

import com.toystorage.backend.dto.request.deliveries.tracking
        .UpdateDeliveryLocationRequest;

import com.toystorage.backend.dto.response.deliveries.tracking
        .DeliveryLocationResponse;

import com.toystorage.backend.dto.response.deliveries.tracking
        .DeliveryTrackingResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.DeliveryLocations;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.deliveries.tracking
        .DeliveryLocationMapper;

import com.toystorage.backend.repository.deliveries
        .DeliveryLocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DeliveryLocationService {

    /*
     * GPS có thể gửi gần mỗi giây,
     * nhưng chỉ persist history mỗi 10 giây.
     */
    private static final long
            HISTORY_INTERVAL_SECONDS =
            10;


    /*
     * Quá 10 giây không nhận GPS mới
     * thì đánh dấu stale.
     */
    private static final long
            STALE_AFTER_SECONDS =
            10;


    private final DeliveryLocationRepository
            locationRepository;

    private final DeliveryTrackingValidationService
            validationService;

    private final DeliveryLocationMapper
            mapper;

    private final SimpMessagingTemplate
            messagingTemplate;


    // =====================================================
    // REALTIME CACHE
    // =====================================================

    /*
     * Vị trí realtime mới nhất.
     *
     * key = deliveryId
     */
    private final ConcurrentHashMap<
            Long,
            DeliveryLocations
            >
            latestLocations =
            new ConcurrentHashMap<>();


    /*
     * Thời gian lần cuối persist DB.
     *
     * key = deliveryId
     */
    private final ConcurrentHashMap<
            Long,
            LocalDateTime
            >
            lastPersistedAt =
            new ConcurrentHashMap<>();


    // =====================================================
    // UPDATE LOCATION
    // =====================================================

    @Transactional
    public DeliveryLocationResponse updateLocation(
            Long deliveryId,
            UpdateDeliveryLocationRequest request
    ) {

        // =================================================
        // CURRENT DRIVER
        // =================================================

        Users driver =
                validationService
                        .getCurrentUser();


        // =================================================
        // DELIVERY
        // =================================================

        Deliveries delivery =
                validationService
                        .getDelivery(
                                deliveryId
                        );


        /*
         * Đảm bảo GPS thuộc đúng driver.
         */
        validationService.validateDriver(
                delivery,
                driver
        );


        /*
         * Chỉ IN_TRANSIT.
         */
        validationService
                .validateCanShareLocation(
                        delivery
                );


        // =================================================
        // TIME
        // =================================================

        LocalDateTime now =
                LocalDateTime.now();


        LocalDateTime recordedAt =
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                        : now;


        /*
         * Cho phép clock thiết bị lệch tối đa 2 phút.
         */
        if (recordedAt.isAfter(
                now.plusMinutes(2)
        )) {

            throw new BadRequest(
                    "Location recorded time cannot be in the future"
            );
        }


        // =================================================
        // BUILD LOCATION
        // =================================================

        DeliveryLocations location =
                DeliveryLocations
                        .builder()

                        .delivery(
                                delivery
                        )

                        .driver(
                                driver
                        )

                        .latitude(
                                request.getLatitude()
                        )

                        .longitude(
                                request.getLongitude()
                        )

                        .accuracyMeters(
                                request.getAccuracyMeters()
                        )

                        .speedMps(
                                request.getSpeedMps()
                        )

                        .heading(
                                request.getHeading()
                        )

                        .recordedAt(
                                recordedAt
                        )

                        .receivedAt(
                                now
                        )

                        .shipmentLocationsCode(
                                generateLocationCode()
                        )

                        .build();


        // =================================================
        // REALTIME CACHE
        // =================================================

        /*
         * Cache luôn cập nhật mỗi GPS request.
         */
        latestLocations.put(
                deliveryId,
                location
        );


        // =================================================
        // DATABASE HISTORY
        // =================================================

        boolean shouldPersist =
                shouldPersist(
                        deliveryId,
                        now
                );


        if (shouldPersist) {

            location =
                    locationRepository
                            .save(
                                    location
                            );


            lastPersistedAt.put(
                    deliveryId,
                    now
            );


            /*
             * Save lại cache để có id DB.
             */
            latestLocations.put(
                    deliveryId,
                    location
            );
        }


        // =================================================
        // RESPONSE
        // =================================================

        DeliveryLocationResponse response =
                mapper.toLocationResponse(
                        location,
                        shouldPersist
                );


        // =================================================
        // WEBSOCKET
        // =================================================

        /*
         * Broadcast realtime.
         *
         * Client subscribe:
         *
         * /topic/deliveries/{deliveryId}/location
         */
        messagingTemplate.convertAndSend(
                "/topic/deliveries/"
                        + deliveryId
                        + "/location",

                response
        );


        return response;
    }


    // =====================================================
    // GET LATEST LOCATION
    // =====================================================

    @Transactional(readOnly = true)
    public DeliveryTrackingResponse getLatestLocation(
            Long deliveryId
    ) {

        // =================================================
        // USER
        // =================================================

        Users user =
                validationService
                        .getCurrentUser();


        // =================================================
        // DELIVERY
        // =================================================

        Deliveries delivery =
                validationService
                        .getDelivery(
                                deliveryId
                        );


        /*
         * Kiểm quyền xem chuyến.
         */
        validationService.validateCanView(
                delivery,
                user
        );


        // =================================================
        // CACHE FIRST
        // =================================================

        DeliveryLocations latest =
                latestLocations.get(
                        deliveryId
                );


        // =================================================
        // DB FALLBACK
        // =================================================

        /*
         * Server restart làm mất RAM cache.
         * Khi đó lấy điểm gần nhất từ DB.
         */
        if (latest == null) {

            latest =
                    locationRepository
                            .findTopByDeliveryIdOrderByRecordedAtDesc(
                                    deliveryId
                            )

                            .orElse(null);
        }


        // =================================================
        // LAST UPDATE
        // =================================================

        Long secondsSinceLastUpdate =
                calculateSecondsSinceLastUpdate(
                        latest
                );


        boolean stale =
                secondsSinceLastUpdate == null
                        || secondsSinceLastUpdate
                        > STALE_AFTER_SECONDS;


        // =================================================
        // MAPPER
        // =================================================

        return mapper.toTrackingResponse(
                delivery,
                latest,
                secondsSinceLastUpdate,
                stale
        );
    }


    // =====================================================
    // STOP TRACKING
    // =====================================================

    /*
     * Gọi khi chuyến:
     *
     * DELIVERED
     * FAILED
     * CANCELLED
     *
     * để giải phóng RAM.
     *
     * Backend vẫn chặn GPS bằng status,
     * nên stopTracking chủ yếu là cleanup.
     */
    public void stopTracking(
            Long deliveryId
    ) {

        latestLocations.remove(
                deliveryId
        );


        lastPersistedAt.remove(
                deliveryId
        );
    }


    // =====================================================
    // SHOULD PERSIST
    // =====================================================

    private boolean shouldPersist(
            Long deliveryId,
            LocalDateTime now
    ) {

        LocalDateTime previous =
                lastPersistedAt.get(
                        deliveryId
                );


        // =================================================
        // CACHE KHÔNG CÓ
        // =================================================

        if (previous == null) {

            /*
             * Có thể do:
             *
             * - location đầu tiên
             * - backend vừa restart
             *
             * nên kiểm DB trước.
             */
            DeliveryLocations latestDatabaseLocation =
                    locationRepository
                            .findTopByDeliveryIdOrderByRecordedAtDesc(
                                    deliveryId
                            )

                            .orElse(null);


            /*
             * Chưa từng lưu GPS.
             *
             * Điểm đầu tiên luôn persist.
             */
            if (latestDatabaseLocation == null) {

                return true;
            }


            /*
             * Có DB history rồi.
             *
             * Ưu tiên receivedAt.
             */
            if (latestDatabaseLocation
                    .getReceivedAt() != null) {

                previous =
                        latestDatabaseLocation
                                .getReceivedAt();
            }
            else {

                previous =
                        latestDatabaseLocation
                                .getRecordedAt();
            }


            lastPersistedAt.put(
                    deliveryId,
                    previous
            );
        }


        // =================================================
        // CALCULATE INTERVAL
        // =================================================

        long seconds =
                Duration
                        .between(
                                previous,
                                now
                        )
                        .getSeconds();


        return seconds
                >= HISTORY_INTERVAL_SECONDS;
    }


    // =====================================================
    // SECONDS SINCE LAST UPDATE
    // =====================================================

    private Long calculateSecondsSinceLastUpdate(
            DeliveryLocations latest
    ) {

        if (latest == null) {

            return null;
        }


        LocalDateTime lastUpdate =
                latest.getReceivedAt() != null
                        ? latest.getReceivedAt()
                        : latest.getRecordedAt();


        if (lastUpdate == null) {

            return null;
        }


        long seconds =
                Duration
                        .between(
                                lastUpdate,
                                LocalDateTime.now()
                        )
                        .getSeconds();


        /*
         * Nếu clock bị lệch nhẹ,
         * không trả số âm cho frontend.
         */
        return Math.max(
                seconds,
                0L
        );
    }


    // =====================================================
    // GENERATE LOCATION CODE
    // =====================================================

    private String generateLocationCode() {

        return "LOC-"
                + UUID
                .randomUUID()
                .toString()
                .replace(
                        "-",
                        ""
                )
                .substring(
                        0,
                        16
                )
                .toUpperCase();
    }
}