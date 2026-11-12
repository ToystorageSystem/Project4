package com.toystorage.backend.controllers.deliveries.fleet;

import com.toystorage.backend.services.deliveries.fleet.FleetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Vehicles, driver availability/GPS and dispatch suggestions (driver app + web "Vehicles & routes"). */
@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    public record FlagRequest(Boolean value) {
    }

    public record PositionRequest(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters) {
    }

    public record SiteRequest(BigDecimal latitude, BigDecimal longitude) {
    }

    public record VehicleRequest(String plate, Long driverId, Integer capacityPackages) {
    }

    public record AssignRequest(Long vehicleId) {
    }

    @GetMapping("/board")
    public Map<String, Object> board() {
        return fleetService.board();
    }

    @GetMapping("/vehicles/{vehicleId}/route")
    public Map<String, Object> route(@PathVariable Long vehicleId) {
        return fleetService.route(vehicleId);
    }

    @GetMapping("/recommendations/{manifestId}")
    public List<Map<String, Object>> recommendations(@PathVariable Long manifestId) {
        return fleetService.recommendations(manifestId);
    }

    @PostMapping("/manifests/{manifestId}/assign")
    public Object assign(@PathVariable Long manifestId, @RequestBody AssignRequest request) {
        return fleetService.assign(manifestId, request == null ? null : request.vehicleId());
    }

    @PostMapping("/vehicles")
    public Map<String, Object> register(@RequestBody VehicleRequest request) {
        return fleetService.registerVehicle(request.plate(), request.driverId(), request.capacityPackages());
    }

    @PutMapping("/sites/{warehouseId}")
    public Map<String, Object> site(@PathVariable Long warehouseId, @RequestBody SiteRequest request) {
        return fleetService.setSite(warehouseId, request.latitude(), request.longitude());
    }

    @PatchMapping("/me/available")
    public Map<String, Object> available(@RequestBody FlagRequest request) {
        return fleetService.setAvailable(request != null && Boolean.TRUE.equals(request.value()));
    }

    @PatchMapping("/me/sharing")
    public Map<String, Object> sharing(@RequestBody FlagRequest request) {
        return fleetService.setSharing(request != null && Boolean.TRUE.equals(request.value()));
    }

    @PostMapping("/me/location")
    public Map<String, Object> location(@RequestBody PositionRequest request) {
        return fleetService.position(request.latitude(), request.longitude(), request.accuracyMeters());
    }
}
