package com.toystorage.backend.services.deliveries.fleet;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.DriverPresence;
import com.toystorage.backend.entity.deliveries.SiteCoordinate;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.repository.deliveries.DriverPresenceRepository;
import com.toystorage.backend.repository.deliveries.SiteCoordinateRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Shared geography helpers for dispatching: where a site is, where a driver is,
 * straight-line distance and how many packages a delivery carries.
 */
@Service
@RequiredArgsConstructor
public class FleetGeoService {

    /** A GPS fix older than this is shown as "last known" instead of live. */
    public static final int LIVE_MINUTES = 10;

    private final SiteCoordinateRepository siteCoordinateRepository;
    private final DriverPresenceRepository driverPresenceRepository;
    private final ShipmentManifestPackageRepository shipmentManifestPackageRepository;

    public record Point(double latitude, double longitude) {
    }

    /** source: LIVE_GPS, LAST_KNOWN or WAREHOUSE_BASE. */
    public record DriverPosition(Point point, String source, LocalDateTime receivedAt) {
    }

    @Transactional(readOnly = true)
    public Optional<Point> siteOf(Warehouses warehouse) {
        if (warehouse == null) return Optional.empty();
        return siteCoordinateRepository.findByWarehouse_Id(warehouse.getId())
                .map(FleetGeoService::toPoint);
    }

    @Transactional(readOnly = true)
    public Optional<DriverPresence> presenceOf(Users driver) {
        return driverPresenceRepository.findByDriver_Id(driver.getId());
    }

    /** Driver is accepting new pickups unless he switched it off in the app. */
    @Transactional(readOnly = true)
    public boolean isAvailable(Users driver) {
        return presenceOf(driver).map(DriverPresence::isAvailable).orElse(true);
    }

    @Transactional(readOnly = true)
    public Optional<DriverPosition> positionOf(Users driver) {
        Optional<DriverPresence> presence = presenceOf(driver);
        if (presence.isPresent() && presence.get().getLatitude() != null && presence.get().getLongitude() != null) {
            DriverPresence p = presence.get();
            boolean live = p.isSharing() && p.getReceivedAt() != null
                    && p.getReceivedAt().isAfter(LocalDateTime.now().minusMinutes(LIVE_MINUTES));
            return Optional.of(new DriverPosition(
                    new Point(p.getLatitude().doubleValue(), p.getLongitude().doubleValue()),
                    live ? "LIVE_GPS" : "LAST_KNOWN",
                    p.getReceivedAt()));
        }
        return siteOf(driver.getWarehouse()).map(point -> new DriverPosition(point, "WAREHOUSE_BASE", null));
    }

    @Transactional(readOnly = true)
    public int packagesOf(Deliveries delivery) {
        if (delivery == null || delivery.getManifest() == null) return 0;
        return shipmentManifestPackageRepository.findByManifestId(delivery.getManifest().getId()).size();
    }

    /** Great-circle distance in km. */
    public static double distanceKm(Point a, Point b) {
        double r = 6371.0;
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    private static Point toPoint(SiteCoordinate c) {
        return new Point(c.getLatitude().doubleValue(), c.getLongitude().doubleValue());
    }
}
