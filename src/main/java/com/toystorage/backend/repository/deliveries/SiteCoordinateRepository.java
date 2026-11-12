package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.SiteCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteCoordinateRepository extends JpaRepository<SiteCoordinate, Long> {

    Optional<SiteCoordinate> findByWarehouse_Id(Long warehouseId);
}
