package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface ShipmentManifestPackageRepository
        extends JpaRepository<ShipmentManifestPackage, Long> {


    List<ShipmentManifestPackage>
    findByManifestId(Long manifestId);
}