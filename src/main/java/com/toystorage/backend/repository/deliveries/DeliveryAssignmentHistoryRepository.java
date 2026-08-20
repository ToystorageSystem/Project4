package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryAssignmentHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentHistoryRepository
        extends JpaRepository<
        DeliveryAssignmentHistory,
        Long
        > {

    boolean
    existsByDeliveryIdAndDriverIdAndRejectedAtIsNotNull(
            Long deliveryId,
            Long driverId
    );


    Optional<DeliveryAssignmentHistory>
    findTopByDeliveryIdAndDriverIdOrderByAssignedAtDesc(
            Long deliveryId,
            Long driverId
    );


    List<DeliveryAssignmentHistory>
    findByDeliveryIdOrderByAssignedAtDesc(
            Long deliveryId
    );
}