package com.toystorage.backend.services.deliveries.assignment;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.repository.deliveries.DeliveryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverScoringService {

    private final DeliveryRepository
            deliveryRepository;


    /*
     * Điểm càng cao càng ưu tiên.
     */
    public double calculateScore(
            Users driver,
            Deliveries delivery
    ) {

        double score = 100.0;


        // =====================================================
        // DRIVER ĐANG GIAO HÀNG
        // =====================================================

        boolean currentlyDelivering =
                deliveryRepository
                        .existsByDriverIdAndDeliveryStatusIn(
                                driver.getId(),
                                List.of(
                                        DeliveryStatus.IN_TRANSIT,
                                        DeliveryStatus.ARRIVED
                                )
                        );


        if (currentlyDelivering) {
            return -1;
        }


        // =====================================================
        // WORKLOAD
        // =====================================================

        long activeDeliveries =
                deliveryRepository
                        .countByDriverIdAndDeliveryStatusIn(
                                driver.getId(),
                                List.of(
                                        DeliveryStatus.ASSIGNED,
                                        DeliveryStatus.ACCEPTED,
                                        DeliveryStatus.READY_TO_SHIP
                                )
                        );


        /*
         * Càng nhiều chuyến đang chờ
         * thì càng giảm ưu tiên.
         */
        score -= activeDeliveries * 20.0;


        // =====================================================
        // SAME PICKUP WAREHOUSE
        // =====================================================

        if (driver.getWarehouse() != null
                && delivery.getFromWarehouse() != null
                && driver.getWarehouse()
                .getId()
                .equals(
                        delivery
                                .getFromWarehouse()
                                .getId()
                )) {

            score += 20.0;
        }


        // =====================================================
        // FUTURE AI
        // =====================================================

        /*
         * Khi bật AI sau này:
         *
         * double aiScore =
         *         aiDriverScoringService.predictScore(
         *                 driver,
         *                 delivery
         *         );
         *
         * score =
         *         score * 0.5
         *         + aiScore * 0.5;
         */


        return score;
    }
}