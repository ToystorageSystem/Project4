package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface ShipmentManifestTransferRepository
        extends JpaRepository<ShipmentManifestTransfer, Long> {


    Optional<ShipmentManifestTransfer>
    findByTransferId(Long transferId);
}