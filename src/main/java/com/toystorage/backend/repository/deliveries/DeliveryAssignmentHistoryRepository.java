package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryAssignmentHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAssignmentHistoryRepository
        extends JpaRepository<DeliveryAssignmentHistory, Long> {

    @EntityGraph(attributePaths = {
            "driver"
    })
    List<DeliveryAssignmentHistory> findByDelivery_IdOrderByAssignedAtAsc(
            Long deliveryId
    );
}
