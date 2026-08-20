/*
package com.toystorage.backend.services.deliveries.assignment;

import com.toystorage.backend.entity.deliveries.Deliveries;

import com.toystorage.backend.entity.users.Users;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AiDriverScoringService {


    public Users selectBestDriver(
            List<Users> candidates,
            Deliveries delivery
    ) {

        return candidates.stream()

                .max(
                        Comparator.comparingDouble(
                                driver ->
                                        predictScore(
                                                driver,
                                                delivery
                                        )
                        )
                )

                .orElseThrow();
    }


    public double predictScore(
            Users driver,
            Deliveries delivery
    ) {

        /*
         * =============================================
         * FUTURE AI / ML
         * =============================================
         *
         * Có thể gửi các feature:
         *
         * driverId
         * currentActiveTrips
         * pickupWarehouseId
         * destinationWarehouseId
         * distanceToPickup
         * historicalOnTimeRate
         * rejectionRate
         * averageDeliveryMinutes
         * expectedPickupAt
         * expectedDeliveryAt
         *
         * sang Python ML Service / AI Service.
         *
         *
         * Ví dụ:
         *
         * AiDriverPredictionRequest request =
         *         AiDriverPredictionRequest.builder()
         *                 .driverId(driver.getId())
         *                 .deliveryId(delivery.getId())
         *                 ...
         *                 .build();
         *
         *
         * AiDriverPredictionResponse response =
         *         aiClient.predict(request);
         *
         *
         * return response.getScore();



        return 50.0;
                }
}
*/
