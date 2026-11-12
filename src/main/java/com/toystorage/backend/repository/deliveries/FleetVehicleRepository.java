package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, Long> {

    @Query("SELECT v FROM FleetVehicle v JOIN FETCH v.driver WHERE v.active = true ORDER BY v.plate")
    List<FleetVehicle> findActive();

    @Query("SELECT v FROM FleetVehicle v JOIN FETCH v.driver d WHERE v.active = true AND d.id = :driverId")
    Optional<FleetVehicle> findActiveByDriverId(@Param("driverId") Long driverId);

    Optional<FleetVehicle> findByDriver_Id(Long driverId);

    boolean existsByPlateIgnoreCase(String plate);
}
