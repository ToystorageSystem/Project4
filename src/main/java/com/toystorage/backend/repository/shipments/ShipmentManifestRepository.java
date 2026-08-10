package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifests;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentManifestRepository
        extends JpaRepository<ShipmentManifests, Long> {
}