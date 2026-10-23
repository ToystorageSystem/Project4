package com.toystorage.backend.repository.shipments;

import org.springframework.data.jpa.repository.EntityGraph;
import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface ShipmentManifestPackageRepository
        extends JpaRepository<ShipmentManifestPackage, Long> {


    List<ShipmentManifestPackage>
    findByManifestId(Long manifestId);


    // Methods restored for legacy business APIs without replacing existing repository logic.

    // =====================================================
        // MANIFEST SUMMARY
        // =====================================================

        /**
         * Đếm số kiện hàng thuộc một bảng kê.
         */
        long countByManifest_Id(
                Long manifestId
        );
}