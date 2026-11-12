package com.toystorage.backend.repository.deliveries;

import com.toystorage.backend.entity.deliveries.DriverPresence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverPresenceRepository extends JpaRepository<DriverPresence, Long> {

    Optional<DriverPresence> findByDriver_Id(Long driverId);
}
