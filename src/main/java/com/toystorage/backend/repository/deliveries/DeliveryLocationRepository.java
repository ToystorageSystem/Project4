package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryLocations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryLocationRepository
        extends JpaRepository<DeliveryLocations, Long> {

    Optional<DeliveryLocations>
    findTopByDeliveryIdOrderByRecordedAtDesc(
            Long deliveryId
    );

    List<DeliveryLocations>
    findByDeliveryIdOrderByRecordedAtAsc(
            Long deliveryId
    );

    long countByDeliveryId(
            Long deliveryId
    );
}