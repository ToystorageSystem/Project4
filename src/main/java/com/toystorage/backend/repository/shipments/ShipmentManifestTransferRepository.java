package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentManifestTransferRepository
        extends JpaRepository<ShipmentManifestTransfer, Long> {
}