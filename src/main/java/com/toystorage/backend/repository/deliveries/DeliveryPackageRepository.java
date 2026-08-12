package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryPackages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface DeliveryPackageRepository
        extends JpaRepository<DeliveryPackages, Long> {

    List<DeliveryPackages> findByDeliveryId(Long deliveryId);
}