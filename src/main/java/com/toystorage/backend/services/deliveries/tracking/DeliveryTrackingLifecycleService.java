package com.toystorage.backend.services.deliveries.tracking;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryTrackingLifecycleService {

    private final DeliveryLocationService
            deliveryLocationService;


    // =====================================================
    // STOP TRACKING
    // =====================================================

    /*
     * Gọi khi chuyến kết thúc:
     *
     * DELIVERED
     * FAILED
     * CANCELLED
     *
     * Mục đích:
     * - xóa realtime cache
     * - xóa lastPersistedAt cache
     *
     * Database history vẫn giữ nguyên.
     */
    public void stopTracking(
            Long deliveryId
    ) {

        deliveryLocationService
                .stopTracking(
                        deliveryId
                );
    }
}