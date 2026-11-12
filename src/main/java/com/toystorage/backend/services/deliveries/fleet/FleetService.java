package com.toystorage.backend.services.deliveries.fleet;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.DriverPresence;
import com.toystorage.backend.entity.deliveries.FleetVehicle;
import com.toystorage.backend.entity.deliveries.SiteCoordinate;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.auth.UserRoleRepository;
import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.deliveries.DriverPresenceRepository;
import com.toystorage.backend.repository.deliveries.FleetVehicleRepository;
import com.toystorage.backend.repository.deliveries.SiteCoordinateRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.services.deliveries.assignment.DeliveryAssignmentService;
import com.toystorage.backend.services.deliveries.assignment.DriverScoringService;
import com.toystorage.backend.services.warehouses.manager.ManagerOpsAccess;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vehicles & routes: the backend of the driver app "Vehicle & route" screen and the web Fleet page.
 * Distances are straight-line (haversine); no external map service or API key is needed.
 */
@Service
@RequiredArgsConstructor
public class FleetService {

    static final String DRIVER_ROLE = "DELIVERY_STAFF";
    static final Set<String> DRIVER_ROLES = Set.of("DELIVERY_STAFF", "DRIVER");
    static final Set<String> SITE_ROLES = Set.of("WAREHOUSE_MANAGER", "WH_MANAGER", "STORE_MANAGER");
    static final List<DeliveryStatus> ACTIVE = List.of(
            DeliveryStatus.ASSIGNED, DeliveryStatus.ACCEPTED, DeliveryStatus.READY_TO_SHIP,
            DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED);
    static final Set<DeliveryStatus> NOT_PICKED_UP = EnumSet.of(
            DeliveryStatus.ASSIGNED, DeliveryStatus.ACCEPTED, DeliveryStatus.READY_TO_SHIP);

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final DeliveryRepository deliveryRepository;
    private final FleetVehicleRepository vehicleRepository;
    private final DriverPresenceRepository presenceRepository;
    private final SiteCoordinateRepository siteRepository;
    private final FleetGeoService geo;
    private final DriverScoringService scoringService;
    private final DeliveryAssignmentService assignmentService;
    private final ManagerOpsAccess access;

    // =====================================================================
    // BOARD
    // =====================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> board() {
        Users me = access.currentUser();
        boolean driver = hasRole(DRIVER_ROLES);
        boolean dispatcher = canDispatch();
        Map<Long, SiteCoordinate> coordinates = siteRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getWarehouse().getId(), Function.identity(), (a, b) -> a));

        List<Map<String, Object>> sites = entityManager.createQuery(
                        "SELECT w FROM Warehouses w WHERE w.status = :active ORDER BY w.name", Warehouses.class)
                .setParameter("active", WarehouseStatus.ACTIVE)
                .getResultList().stream()
                .map(w -> {
                    SiteCoordinate c = coordinates.get(w.getId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", w.getId());
                    m.put("name", w.getName());
                    m.put("type", w.getType() == null ? null : w.getType().name());
                    m.put("latitude", c == null ? null : c.getLatitude());
                    m.put("longitude", c == null ? null : c.getLongitude());
                    return m;
                }).toList();

        List<FleetVehicle> vehicles = driver && !dispatcher
                ? vehicleRepository.findActiveByDriverId(me.getId()).map(List::of).orElse(List.of())
                : vehicleRepository.findActive();

        Map<String, Object> board = new LinkedHashMap<>();
        board.put("myWarehouseId", me.getWarehouse() == null ? null : me.getWarehouse().getId());
        board.put("isDriver", driver);
        board.put("canDispatch", dispatcher);
        board.put("sites", sites);
        board.put("vehicles", vehicles.stream().map(this::vehicleView).toList());
        if (driver) {
            // A driver without a registered vehicle still sees his own availability switches.
            board.put("me", presenceView(me));
        }
        if (dispatcher) {
            Set<Long> withVehicle = vehicleRepository.findActive().stream()
                    .map(v -> v.getDriver().getId()).collect(Collectors.toSet());
            board.put("drivers", userRepository.findActiveUsersByRoleCode(DRIVER_ROLE).stream()
                    .filter(u -> !withVehicle.contains(u.getId()))
                    .map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getName()))
                    .toList());
            board.put("readyManifests", me.getWarehouse() == null ? List.of() : readyManifests(me.getWarehouse().getId()));
        } else {
            board.put("drivers", List.of());
            board.put("readyManifests", List.of());
        }
        return board;
    }

    private List<Map<String, Object>> readyManifests(Long warehouseId) {
        return entityManager.createQuery("""
                        SELECT d FROM Deliveries d
                        WHERE d.fromWarehouse.id = :warehouseId
                          AND d.deliveryStatus IN :statuses
                          AND d.manifest IS NOT NULL
                          AND d.handedOverAt IS NULL
                        ORDER BY d.createdAt
                        """, Deliveries.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("statuses", List.of(DeliveryStatus.CREATED, DeliveryStatus.REJECTED))
                .getResultList().stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", d.getManifest().getId());
                    m.put("deliveryId", d.getId());
                    m.put("manifestCode", d.getManifest().getManifestCode());
                    m.put("shipmentCode", d.getShipmentCode());
                    m.put("packageCount", geo.packagesOf(d));
                    m.put("fromName", d.getFromWarehouse().getName());
                    m.put("toName", d.getToWarehouse().getName());
                    m.put("status", d.getDeliveryStatus().name());
                    return m;
                }).toList();
    }

    private Map<String, Object> vehicleView(FleetVehicle v) {
        Users driver = v.getDriver();
        Optional<DriverPresence> presence = geo.presenceOf(driver);
        Optional<FleetGeoService.DriverPosition> position = geo.positionOf(driver);
        RoutePlan plan = plan(driver, null);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("plate", v.getPlate());
        m.put("driverId", driver.getId());
        m.put("driverName", driver.getName());
        m.put("capacityPackages", v.getCapacityPackages());
        m.put("available", presence.map(DriverPresence::isAvailable).orElse(true));
        m.put("sharing", presence.map(DriverPresence::isSharing).orElse(false));
        m.put("stale", position.map(p -> !"LIVE_GPS".equals(p.source())).orElse(true));
        m.put("positionSource", position.map(FleetGeoService.DriverPosition::source).orElse("UNKNOWN"));
        m.put("latitude", position.map(p -> p.point().latitude()).orElse(null));
        m.put("longitude", position.map(p -> p.point().longitude()).orElse(null));
        m.put("receivedAt", position.map(FleetGeoService.DriverPosition::receivedAt).orElse(null));
        m.put("stops", plan.stops());
        m.put("peakLoad", plan.peakLoad());
        return m;
    }

    private Map<String, Object> presenceView(Users me) {
        Optional<DriverPresence> presence = geo.presenceOf(me);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", presence.map(DriverPresence::isAvailable).orElse(true));
        m.put("sharing", presence.map(DriverPresence::isSharing).orElse(false));
        m.put("receivedAt", presence.map(DriverPresence::getReceivedAt).orElse(null));
        return m;
    }

    // =====================================================================
    // ROUTE
    // =====================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> route(Long vehicleId) {
        Users me = access.currentUser();
        FleetVehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFound("Vehicle not found: " + vehicleId));
        if (!canDispatch() && !v.getDriver().getId().equals(me.getId())) {
            throw new Forbidden("You can only view your own route");
        }
        RoutePlan plan = plan(v.getDriver(), null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vehicleId", v.getId());
        m.put("plate", v.getPlate());
        m.put("driverName", v.getDriver().getName());
        m.put("distanceKm", round(plan.distanceKm()));
        m.put("distanceModel", "STRAIGHT_LINE");
        m.put("visits", plan.visits());
        m.put("roadGeometry", plan.geometry());
        m.put("peakLoad", plan.peakLoad());
        return m;
    }

    // =====================================================================
    // RECOMMENDATIONS ("Find vehicle")
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recommendations(Long manifestId) {
        requireDispatcher();
        Deliveries delivery = deliveryOfManifest(manifestId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FleetVehicle v : vehicleRepository.findActive()) {
            Users driver = v.getDriver();
            if (driver.getStatus() != UserStatus.ACTIVE) continue;
            DriverScoringService.DriverScore score = scoringService.explain(driver, delivery);
            if (score.score() < 0) continue;
            RoutePlan without = plan(driver, null);
            RoutePlan with = plan(driver, delivery);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vehicleId", v.getId());
            m.put("plate", v.getPlate());
            m.put("driverId", driver.getId());
            m.put("driverName", driver.getName());
            m.put("activeDeliveries", score.activeDeliveries());
            m.put("peakLoad", with.peakLoad());
            m.put("capacityPackages", v.getCapacityPackages());
            m.put("extraKm", round(Math.max(0, with.distanceKm() - without.distanceKm())));
            m.put("distanceToPickupKm", score.distanceKm() == null ? null : round(score.distanceKm()));
            m.put("positionSource", score.positionSource());
            m.put("distanceModel", "STRAIGHT_LINE");
            m.put("score", round(score.score()));
            result.add(m);
        }
        result.sort(Comparator.comparingDouble((Map<String, Object> m) -> ((Number) m.get("score")).doubleValue()).reversed());
        return result;
    }

    @Transactional
    public Object assign(Long manifestId, Long vehicleId) {
        Users me = requireDispatcher();
        Deliveries delivery = deliveryOfManifest(manifestId);
        if (!hasRole(ManagerOpsAccess.BUSINESS_ROLES)
                && (me.getWarehouse() == null || !me.getWarehouse().getId().equals(delivery.getFromWarehouse().getId()))) {
            throw new Forbidden("You can only dispatch shipments from your own warehouse");
        }
        if (vehicleId == null) throw new BadRequest("Choose a vehicle");
        FleetVehicle v = vehicleRepository.findById(vehicleId)
                .filter(FleetVehicle::isActive)
                .orElseThrow(() -> new NotFound("Vehicle not found: " + vehicleId));
        return assignmentService.assignToDriver(delivery.getId(), v.getDriver());
    }

    private Deliveries deliveryOfManifest(Long manifestId) {
        return deliveryRepository.findTopByManifestIdOrderByCreatedAtDesc(manifestId)
                .orElseThrow(() -> new NotFound("No delivery trip for manifest " + manifestId));
    }

    // =====================================================================
    // VEHICLES & SITES
    // =====================================================================

    @Transactional
    public Map<String, Object> registerVehicle(String plate, Long driverId, Integer capacity) {
        requireDispatcher();
        String cleanPlate = plate == null ? "" : plate.trim().toUpperCase(Locale.ROOT);
        if (cleanPlate.isEmpty() || cleanPlate.length() > 30) throw new BadRequest("Enter a license plate (max 30 characters)");
        if (capacity == null || capacity < 1 || capacity > 10_000) throw new BadRequest("Capacity must be between 1 and 10000 packages");
        Users driver = userRepository.findById(driverId == null ? -1L : driverId)
                .orElseThrow(() -> new NotFound("Driver not found"));
        boolean isDriver = userRoleRepository.findByUser_Id(driver.getId()).stream()
                .map(ur -> ur.getRole())
                .anyMatch(r -> r != null && DRIVER_ROLES.contains(ManagerOpsAccess.normalize(r.getRoleCode())));
        if (!isDriver || driver.getStatus() != UserStatus.ACTIVE) throw new BadRequest("Choose an active Delivery Staff");

        FleetVehicle vehicle = vehicleRepository.findByDriver_Id(driver.getId()).orElseGet(FleetVehicle::new);
        if (vehicle.getId() != null && vehicle.isActive()) throw new BadRequest(driver.getName() + " already has a vehicle");
        if (!cleanPlate.equalsIgnoreCase(vehicle.getPlate()) && vehicleRepository.existsByPlateIgnoreCase(cleanPlate)) {
            throw new BadRequest("License plate " + cleanPlate + " is already registered");
        }
        vehicle.setPlate(cleanPlate);
        vehicle.setDriver(driver);
        vehicle.setCapacityPackages(capacity);
        vehicle.setActive(true);
        vehicle = vehicleRepository.save(vehicle);
        return vehicleView(vehicle);
    }

    @Transactional
    public Map<String, Object> setSite(Long warehouseId, BigDecimal latitude, BigDecimal longitude) {
        Users me = access.currentUser();
        boolean admin = hasRole(ManagerOpsAccess.BUSINESS_ROLES);
        boolean own = me.getWarehouse() != null && me.getWarehouse().getId().equals(warehouseId) && hasRole(SITE_ROLES);
        if (!admin && !own) throw new Forbidden("You can only set the coordinates of your own warehouse or store");
        validateCoordinate(latitude, longitude);
        Warehouses warehouse = entityManager.find(Warehouses.class, warehouseId);
        if (warehouse == null) throw new NotFound("Warehouse not found: " + warehouseId);
        SiteCoordinate c = siteRepository.findByWarehouse_Id(warehouseId).orElseGet(SiteCoordinate::new);
        c.setWarehouse(warehouse);
        c.setLatitude(latitude.setScale(7, RoundingMode.HALF_UP));
        c.setLongitude(longitude.setScale(7, RoundingMode.HALF_UP));
        c.setUpdatedBy(me);
        c.setUpdatedAt(LocalDateTime.now());
        siteRepository.save(c);
        return Map.of("id", warehouseId, "latitude", c.getLatitude(), "longitude", c.getLongitude());
    }

    // =====================================================================
    // DRIVER: availability, GPS sharing, position
    // =====================================================================

    @Transactional
    public Map<String, Object> setAvailable(boolean value) {
        DriverPresence p = myPresence();
        p.setAvailable(value);
        presenceRepository.save(p);
        return presenceView(p.getDriver());
    }

    @Transactional
    public Map<String, Object> setSharing(boolean value) {
        DriverPresence p = myPresence();
        p.setSharing(value);
        presenceRepository.save(p);
        return presenceView(p.getDriver());
    }

    @Transactional
    public Map<String, Object> position(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy) {
        validateCoordinate(latitude, longitude);
        DriverPresence p = myPresence();
        p.setLatitude(latitude.setScale(7, RoundingMode.HALF_UP));
        p.setLongitude(longitude.setScale(7, RoundingMode.HALF_UP));
        p.setAccuracyMeters(accuracy == null ? null : accuracy.setScale(2, RoundingMode.HALF_UP));
        p.setReceivedAt(LocalDateTime.now());
        p.setSharing(true);
        presenceRepository.save(p);
        return presenceView(p.getDriver());
    }

    private DriverPresence myPresence() {
        if (!hasRole(DRIVER_ROLES)) throw new Forbidden("Only Delivery Staff can update driver status");
        Users me = access.currentUser();
        return presenceRepository.findByDriver_Id(me.getId()).orElseGet(() -> {
            DriverPresence p = new DriverPresence();
            p.setDriver(me);
            return p;
        });
    }

    // =====================================================================
    // ROUTE PLANNING (greedy nearest stop, pickup always before its drop-off)
    // =====================================================================

    record RoutePlan(List<Map<String, Object>> visits, List<Map<String, Object>> stops,
                     List<double[]> geometry, double distanceKm, int peakLoad) {
    }

    private record Stop(Deliveries delivery, boolean pickup, FleetGeoService.Point point, int packages) {
    }

    RoutePlan plan(Users driver, Deliveries extra) {
        List<Deliveries> deliveries = new ArrayList<>(
                deliveryRepository.findByDriverIdAndDeliveryStatusInOrderByCreatedAtDesc(driver.getId(), ACTIVE));
        Collections.reverse(deliveries); // oldest first
        if (extra != null && deliveries.stream().noneMatch(d -> d.getId().equals(extra.getId()))) deliveries.add(extra);

        List<Stop> open = new ArrayList<>();
        int load = 0;
        for (Deliveries d : deliveries) {
            int packages = geo.packagesOf(d);
            boolean pickedUp = d != extra && !NOT_PICKED_UP.contains(d.getDeliveryStatus());
            if (pickedUp) load += packages;
            else open.add(new Stop(d, true, geo.siteOf(d.getFromWarehouse()).orElse(null), packages));
            open.add(new Stop(d, false, geo.siteOf(d.getToWarehouse()).orElse(null), packages));
        }

        FleetGeoService.Point current = geo.positionOf(driver).map(FleetGeoService.DriverPosition::point).orElse(null);
        List<double[]> geometry = new ArrayList<>();
        if (current != null) geometry.add(new double[]{current.latitude(), current.longitude()});
        Set<Long> picked = new HashSet<>();
        for (Deliveries d : deliveries) {
            if (!NOT_PICKED_UP.contains(d.getDeliveryStatus()) && d != extra) picked.add(d.getId());
        }

        List<Map<String, Object>> visits = new ArrayList<>();
        Map<Long, int[]> order = new LinkedHashMap<>();
        double distance = 0;
        int peak = load;
        while (!open.isEmpty()) {
            Stop next = null;
            double best = Double.MAX_VALUE;
            for (Stop s : open) {
                if (!s.pickup() && !picked.contains(s.delivery().getId())) continue; // drop only after pickup
                double cost = (current == null || s.point() == null) ? 1e6 : FleetGeoService.distanceKm(current, s.point());
                if (cost < best) {
                    best = cost;
                    next = s;
                }
            }
            if (next == null) break;
            open.remove(next);
            if (next.point() != null) {
                if (current != null) distance += FleetGeoService.distanceKm(current, next.point());
                current = next.point();
                geometry.add(new double[]{current.latitude(), current.longitude()});
            }
            if (next.pickup()) {
                picked.add(next.delivery().getId());
                load += next.packages();
                peak = Math.max(peak, load);
            } else {
                load -= next.packages();
            }
            Warehouses site = next.pickup() ? next.delivery().getFromWarehouse() : next.delivery().getToWarehouse();
            Map<String, Object> visit = new LinkedHashMap<>();
            visit.put("deliveryId", next.delivery().getId());
            visit.put("kind", next.pickup() ? "PICKUP" : "DROP");
            visit.put("label", site.getName() + " · " + next.delivery().getShipmentCode());
            visit.put("latitude", next.point() == null ? null : next.point().latitude());
            visit.put("longitude", next.point() == null ? null : next.point().longitude());
            visit.put("packages", next.packages());
            visits.add(visit);
            order.computeIfAbsent(next.delivery().getId(), k -> new int[]{0, 0})[next.pickup() ? 0 : 1] = visits.size();
        }

        List<Map<String, Object>> stops = deliveries.stream().filter(d -> d != extra).map(d -> {
            int[] o = order.getOrDefault(d.getId(), new int[]{0, 0});
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deliveryId", d.getId());
            m.put("shipmentCode", d.getShipmentCode());
            m.put("status", d.getDeliveryStatus().name());
            m.put("fromName", d.getFromWarehouse().getName());
            m.put("toName", d.getToWarehouse().getName());
            m.put("packages", geo.packagesOf(d));
            m.put("pickupOrder", o[0] == 0 ? null : o[0]);
            m.put("dropOrder", o[1] == 0 ? null : o[1]);
            return m;
        }).toList();
        return new RoutePlan(visits, stops, geometry, distance, peak);
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private Users requireDispatcher() {
        if (!canDispatch()) throw new Forbidden("Only the Warehouse Manager can dispatch vehicles");
        return access.currentUser();
    }

    private boolean canDispatch() {
        return hasRole(ManagerOpsAccess.MANAGER_ROLES) || hasRole(ManagerOpsAccess.BUSINESS_ROLES);
    }

    private static boolean hasRole(Set<String> roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (roles.contains(ManagerOpsAccess.normalize(a.getAuthority()))) return true;
        }
        return false;
    }

    private static void validateCoordinate(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null
                || latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequest("Enter a valid latitude (-90..90) and longitude (-180..180)");
        }
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
