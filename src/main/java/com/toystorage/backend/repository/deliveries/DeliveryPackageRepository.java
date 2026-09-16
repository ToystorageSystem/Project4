package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DeliveryPackages;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryPackageRepository
        extends JpaRepository<DeliveryPackages, Long> {

    /*
     * Task #13:
     * lấy chính xác các kiện thuộc một chuyến Delivery.
     *
     * Fetch luôn package + người đóng gói để phục vụ
     * màn tracking/detail.
     */
    @EntityGraph(attributePaths = {
            "packageEntity",
            "packageEntity.packedBy"
    })
    List<DeliveryPackages> findByDeliveryId(
            Long deliveryId
    );
}