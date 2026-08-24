package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.Deliveries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository
        extends JpaRepository<Deliveries, Long> {

    /**
     * Lấy chuyến vận chuyển mới nhất của một bảng kê.
     */
    Optional<Deliveries> findFirstByManifest_IdOrderByCreatedAtDesc(
            Long manifestId
    );
}