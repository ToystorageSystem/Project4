package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryHandoverItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryHandoverItemRepository
        extends JpaRepository<DeliveryHandoverItem, Long> {

    boolean existsByHandoverIdAndPackageEntityId(
            Long handoverId,
            Long packageId
    );

    List<DeliveryHandoverItem>
    findByHandoverIdOrderByScannedAtAsc(
            Long handoverId
    );
}