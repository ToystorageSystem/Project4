package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.Deliveries;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository
        extends JpaRepository<Deliveries, Long> {
}