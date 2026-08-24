package com.toystorage.backend.repository.shipments;

import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentManifestPackageRepository
        extends JpaRepository<ShipmentManifestPackage, Long> {


    // =====================================================
    // MANIFEST PACKAGES
    // =====================================================

    /**
     * Lấy toàn bộ kiện hàng thuộc một bảng kê.
     *
     * Fetch luôn package và người đóng gói để phục vụ
     * màn chi tiết bảng kê.
     */
    @EntityGraph(attributePaths = {
            "packageEntity",
            "packageEntity.packedBy"
    })
    List<ShipmentManifestPackage> findByManifestId(
            Long manifestId
    );


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