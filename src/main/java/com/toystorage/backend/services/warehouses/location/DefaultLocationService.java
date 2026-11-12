package com.toystorage.backend.services.warehouses.location;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.repository.warehouses.putaway.WarehouseLocationRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Every warehouse needs a receiving area (and a quarantine area for damaged goods).
 * A warehouse created later from Admin has no locations, so receiving failed with
 * "Receiving location not found for warehouse: X" (2026-11-13).
 * This service returns the active location of that type and creates a default one when missing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLocationService {

    private final WarehouseLocationRepository locationRepository;
    private final EntityManager entityManager;

    @Transactional
    public WarehouseLocations ensure(Long warehouseId, WarehouseLocationType type) {
        return locationRepository
                .findFirstByWarehouseIdAndLocationTypeAndStatus(warehouseId, type, WarehouseStatus.ACTIVE)
                .orElseGet(() -> createDefault(warehouseId, type));
    }

    private WarehouseLocations createDefault(Long warehouseId, WarehouseLocationType type) {
        String code = type == WarehouseLocationType.RECEIVING ? "RCV-AUTO" : "QRN-AUTO";
        String name = type == WarehouseLocationType.RECEIVING ? "Receiving area" : "Quarantine area";

        // the code may already exist (e.g. deactivated by a manager) -> reactivate it
        WarehouseLocations existing = locationRepository
                .findByWarehouseIdAndWarehouseCodeIgnoreCase(warehouseId, code)
                .orElse(null);
        if (existing != null) {
            existing.setLocationType(type);
            existing.setStatus(WarehouseStatus.ACTIVE);
            log.info("Reactivated default {} location {} for warehouse {}", type, code, warehouseId);
            return locationRepository.save(existing);
        }

        WarehouseLocations location = new WarehouseLocations();
        location.setWarehouse(entityManager.getReference(Warehouses.class, warehouseId));
        location.setWarehouseCode(code);
        location.setWarehouseLocationsCode("WL-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10).toUpperCase());
        location.setName(name);
        location.setZone(type.name());
        location.setShelf(code);
        location.setLocationType(type);
        location.setStatus(WarehouseStatus.ACTIVE);
        log.info("Created default {} location {} for warehouse {}", type, code, warehouseId);
        return locationRepository.save(location);
    }
}
