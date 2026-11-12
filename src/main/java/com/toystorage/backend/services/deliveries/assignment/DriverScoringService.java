package com.toystorage.backend.services.deliveries.assignment;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.FleetVehicle;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.deliveries.FleetVehicleRepository;
import com.toystorage.backend.services.deliveries.fleet.FleetGeoService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Smart dispatch score (higher = better, negative = cannot take the trip).
 *
 * Factors, like a ride-hailing dispatcher:
 *  - driver switched "accepting pickups" off      -> excluded
 *  - vehicle would exceed its package capacity    -> excluded
 *  - distance from the driver (live GPS, last known position or home warehouse) to the pickup warehouse
 *  - trips already waiting for the driver (workload) and whether he is on the road right now
 *  - live GPS bonus (the position is reliable), registered vehicle bonus
 */
@Service
@RequiredArgsConstructor
public class DriverScoringService {

    static final List<DeliveryStatus> PENDING =
            List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.ACCEPTED, DeliveryStatus.READY_TO_SHIP);
    static final List<DeliveryStatus> ON_ROAD =
            List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED);

    private final DeliveryRepository deliveryRepository;
    private final FleetVehicleRepository fleetVehicleRepository;
    private final FleetGeoService geo;

    public double calculateScore(Users driver, Deliveries delivery) {
        return explain(driver, delivery).score();
    }

    /** Score with the numbers behind it, used by the fleet "Find vehicle" suggestions. */
    public DriverScore explain(Users driver, Deliveries delivery) {

        if (!geo.isAvailable(driver)) {
            return DriverScore.excluded(driver, "Not accepting pickups");
        }

        List<Deliveries> pending = deliveryRepository
                .findByDriverIdAndDeliveryStatusInOrderByCreatedAtDesc(driver.getId(), PENDING)
                .stream()
                .filter(d -> delivery == null || !d.getId().equals(delivery.getId()))
                .toList();
        boolean onRoad = deliveryRepository.existsByDriverIdAndDeliveryStatusIn(driver.getId(), ON_ROAD);

        // ---- capacity -----------------------------------------------------
        Optional<FleetVehicle> vehicle = fleetVehicleRepository.findActiveByDriverId(driver.getId());
        int load = pending.stream().mapToInt(geo::packagesOf).sum() + geo.packagesOf(delivery);
        Integer capacity = vehicle.map(FleetVehicle::getCapacityPackages).orElse(null);
        if (capacity != null && load > capacity) {
            return DriverScore.excluded(driver, "Vehicle full");
        }

        double score = 100.0;

        // ---- workload -----------------------------------------------------
        score -= pending.size() * 15.0;
        if (onRoad) score -= 25.0;

        // ---- distance to the pickup warehouse ---------------------------------
        Optional<FleetGeoService.DriverPosition> position = geo.positionOf(driver);
        Optional<FleetGeoService.Point> pickup = geo.siteOf(delivery == null ? null : delivery.getFromWarehouse());
        Double distanceKm = null;
        if (position.isPresent() && pickup.isPresent()) {
            distanceKm = FleetGeoService.distanceKm(position.get().point(), pickup.get());
            score -= Math.min(distanceKm, 100.0) * 1.5;
            if ("LIVE_GPS".equals(position.get().source())) score += 10.0;
        } else if (driver.getWarehouse() != null && delivery != null && delivery.getFromWarehouse() != null
                && driver.getWarehouse().getId().equals(delivery.getFromWarehouse().getId())) {
            // No coordinates configured: fall back to "driver belongs to the pickup warehouse".
            score += 20.0;
        }

        if (vehicle.isEmpty()) score -= 10.0;

        return new DriverScore(driver, score, pending.size(), load, capacity, distanceKm,
                position.map(FleetGeoService.DriverPosition::source).orElse("UNKNOWN"), null);
    }

    public record DriverScore(Users driver, double score, int activeDeliveries, int load, Integer capacity,
                              Double distanceKm, String positionSource, String excludedReason) {

        static DriverScore excluded(Users driver, String reason) {
            return new DriverScore(driver, -1, 0, 0, null, null, "UNKNOWN", reason);
        }
    }
}
