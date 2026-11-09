package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryHandover;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryHandoverRepository
        extends JpaRepository<DeliveryHandover, Long> {

    Optional<DeliveryHandover>
    findByDeliveryId(Long deliveryId);
}