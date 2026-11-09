package com.toystorage.backend.services.warehouses.location;

import com.toystorage.backend.dto.request.warehouses.location.CreateWarehouseLocationRequest;
import com.toystorage.backend.dto.request.warehouses.location.UpdateWarehouseLocationRequest;
import com.toystorage.backend.dto.request.warehouses.location.UpdateWarehouseLocationStatusRequest;
import com.toystorage.backend.mapper.warehouses.location.WarehouseLocationMapper;
import com.toystorage.backend.dto.response.warehouses.location.WarehouseLocationResponse;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.warehouses.putaway.WarehouseLocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseLocationService {

    private final WarehouseLocationRepository
            warehouseLocationRepository;

    private final UserRepository
            userRepository;
    private final WarehouseLocationMapper
            warehouseLocationMapper;

    /*
     * =====================================================
     * GET CURRENT USER
     * =====================================================
     */

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        ||
                        !authentication.isAuthenticated()
                        ||
                        "anonymousUser".equals(
                                authentication.getPrincipal()
                        )
        ) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }


        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    /*
     * =====================================================
     * GET CURRENT WAREHOUSE ID
     * =====================================================
     */

    private Long getCurrentWarehouseId() {

        Users user =
                getCurrentUser();


        if (user.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to a warehouse"
            );
        }


        return user
                .getWarehouse()
                .getId();
    }


    /*
     * =====================================================
     * LIST LOCATIONS
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<WarehouseLocationResponse>
    getLocations() {

        Long warehouseId =
                getCurrentWarehouseId();


        return warehouseLocationRepository
                .findByWarehouseIdOrderByZoneAscShelfAscWarehouseCodeAsc(
                        warehouseId
                )
                .stream()
                .map(
                        warehouseLocationMapper::toResponse
                )
                .toList();
    }


    /*
     * =====================================================
     * LOCATION DETAIL
     * =====================================================
     */

    @Transactional(readOnly = true)
    public WarehouseLocationResponse getLocation(
            Long id
    ) {

        Long warehouseId =
                getCurrentWarehouseId();


        WarehouseLocations location =
                getEntity(id);


        validateWarehouse(
                location,
                warehouseId
        );


        return warehouseLocationMapper.toResponse(location);
    }


    /*
     * =====================================================
     * CREATE LOCATION
     * =====================================================
     */

    @Transactional
    public WarehouseLocationResponse createLocation(
            CreateWarehouseLocationRequest request
    ) {

        Users currentUser =
                getCurrentUser();


        if (currentUser.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to a warehouse"
            );
        }


        Long warehouseId =
                currentUser
                        .getWarehouse()
                        .getId();


        String locationCode =
                normalizeRequiredCode(
                        request.getWarehouseCode()
                );


        /*
         * Check duplicate location code.
         */
        boolean duplicated =
                warehouseLocationRepository
                        .existsByWarehouseIdAndWarehouseCodeIgnoreCase(
                                warehouseId,
                                locationCode
                        );


        if (duplicated) {

            throw new BadRequest(
                    "Warehouse location code already exists: "
                            + locationCode
            );
        }


        /*
         * Một warehouse chỉ nên có
         * một RECEIVING ACTIVE.
         */
        WarehouseStatus status =
                request.getStatus() != null
                        ? request.getStatus()
                        : WarehouseStatus.ACTIVE;


        if (
                request.getLocationType()
                        == WarehouseLocationType.RECEIVING
                        &&
                        status == WarehouseStatus.ACTIVE
        ) {

            validateNoOtherActiveReceiving(
                    warehouseId,
                    null
            );
        }


        WarehouseLocations location =
                new WarehouseLocations();


        location.setWarehouse(
                currentUser.getWarehouse()
        );


        location.setWarehouseCode(
                locationCode
        );


        location.setWarehouseLocationsCode(
                generateInternalLocationCode()
        );


        location.setName(
                request
                        .getName()
                        .trim()
        );


        location.setZone(
                normalizeNullableCode(
                        request.getZone()
                )
        );


        location.setShelf(
                normalizeNullableCode(
                        request.getShelf()
                )
        );


        location.setLocationType(
                request.getLocationType()
        );


        location.setStatus(
                status
        );


        WarehouseLocations saved =
                warehouseLocationRepository.save(location);

        return warehouseLocationMapper.toResponse(saved);
    }


    /*
     * =====================================================
     * UPDATE LOCATION
     * =====================================================
     */

    @Transactional
    public WarehouseLocationResponse updateLocation(
            Long id,
            UpdateWarehouseLocationRequest request
    ) {

        Long warehouseId =
                getCurrentWarehouseId();


        WarehouseLocations location =
                getEntity(id);


        validateWarehouse(
                location,
                warehouseId
        );


        String locationCode =
                normalizeRequiredCode(
                        request.getWarehouseCode()
                );


        /*
         * Check duplicate nhưng bỏ qua chính record hiện tại.
         */
        boolean duplicated =
                warehouseLocationRepository
                        .existsByWarehouseIdAndWarehouseCodeIgnoreCaseAndIdNot(
                                warehouseId,
                                locationCode,
                                id
                        );


        if (duplicated) {

            throw new BadRequest(
                    "Warehouse location code already exists: "
                            + locationCode
            );
        }


        /*
         * Nếu update thành RECEIVING ACTIVE
         * thì không được có receiving active khác.
         */
        if (
                request.getLocationType()
                        == WarehouseLocationType.RECEIVING
                        &&
                        request.getStatus()
                                == WarehouseStatus.ACTIVE
        ) {

            validateNoOtherActiveReceiving(
                    warehouseId,
                    id
            );
        }


        location.setWarehouseCode(
                locationCode
        );


        location.setName(
                request
                        .getName()
                        .trim()
        );


        location.setZone(
                normalizeNullableCode(
                        request.getZone()
                )
        );


        location.setShelf(
                normalizeNullableCode(
                        request.getShelf()
                )
        );


        location.setLocationType(
                request.getLocationType()
        );


        location.setStatus(
                request.getStatus()
        );


        WarehouseLocations saved =
                warehouseLocationRepository.save(location);

        return warehouseLocationMapper.toResponse(saved);
    }


    /*
     * =====================================================
     * UPDATE STATUS
     * =====================================================
     */

    @Transactional
    public WarehouseLocationResponse updateStatus(
            Long id,
            UpdateWarehouseLocationStatusRequest request
    ) {

        Long warehouseId =
                getCurrentWarehouseId();


        WarehouseLocations location =
                getEntity(id);


        validateWarehouse(
                location,
                warehouseId
        );


        /*
         * Nếu activate receiving location,
         * phải đảm bảo không có receiving active khác.
         */
        if (
                request.getStatus()
                        == WarehouseStatus.ACTIVE
                        &&
                        location.getLocationType()
                                == WarehouseLocationType.RECEIVING
        ) {

            validateNoOtherActiveReceiving(
                    warehouseId,
                    id
            );
        }


        location.setStatus(
                request.getStatus()
        );


        WarehouseLocations saved =
                warehouseLocationRepository
                        .save(location);


        return warehouseLocationMapper.toResponse(saved);
    }


    /*
     * =====================================================
     * FIND RECEIVING LOCATION
     * INTERNAL USE
     * =====================================================
     */

    @Transactional(readOnly = true)
    public WarehouseLocations getReceivingLocation(
            Long warehouseId
    ) {

        return warehouseLocationRepository
                .findFirstByWarehouseIdAndLocationTypeAndStatus(
                        warehouseId,
                        WarehouseLocationType.RECEIVING,
                        WarehouseStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Receiving location not found for warehouse: "
                                        + warehouseId
                        )
                );
    }


    /*
     * =====================================================
     * RECEIVING LOCATION RESPONSE
     * =====================================================
     */

    @Transactional(readOnly = true)
    public WarehouseLocationResponse
    getReceivingLocationResponse(
            Long warehouseId
    ) {

        validateCurrentWarehouse(
                warehouseId
        );


        WarehouseLocations location =
                getReceivingLocation(
                        warehouseId
                );


        return warehouseLocationMapper.toResponse(location);
    }


    /*
     * =====================================================
     * GET ACTIVE PUTAWAY LOCATION
     * INTERNAL USE
     * =====================================================
     */

    @Transactional(readOnly = true)
    public WarehouseLocations getPutawayLocation(
            Long warehouseId,
            String locationCode
    ) {

        String normalizedCode =
                normalizeRequiredCode(
                        locationCode
                );


        WarehouseLocations location =
                warehouseLocationRepository
                        .findFirstByWarehouseIdAndWarehouseCodeAndStatus(
                                warehouseId,
                                normalizedCode,
                                WarehouseStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Active warehouse location not found: "
                                                + normalizedCode
                                )
                        );


        /*
         * Không cho Staff putaway hàng tốt
         * vào damaged/return/quarantine/receiving.
         */
        if (
                location.getLocationType()
                        != WarehouseLocationType.NORMAL
        ) {

            throw new BadRequest(
                    "Putaway destination must be a NORMAL location"
            );
        }


        return location;
    }


    /*
     * =====================================================
     * API FIND LOCATION BY CODE
     * =====================================================
     */

    @Transactional(readOnly = true)
    public WarehouseLocationResponse getLocationByCode(
            Long warehouseId,
            String locationCode
    ) {

        validateCurrentWarehouse(
                warehouseId
        );


        String normalizedCode =
                normalizeRequiredCode(
                        locationCode
                );


        WarehouseLocations location =
                warehouseLocationRepository
                        .findByWarehouseIdAndWarehouseCodeIgnoreCase(
                                warehouseId,
                                normalizedCode
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Warehouse location not found: "
                                                + normalizedCode
                                )
                        );


        return warehouseLocationMapper.toResponse(location);
    }


    /*
     * =====================================================
     * FIND ENTITY
     * =====================================================
     */

    private WarehouseLocations getEntity(
            Long id
    ) {

        return warehouseLocationRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFound(
                                "Warehouse location not found: "
                                        + id
                        )
                );
    }


    /*
     * =====================================================
     * VALIDATE CURRENT WAREHOUSE
     * =====================================================
     */

    private void validateCurrentWarehouse(
            Long warehouseId
    ) {

        Long currentWarehouseId =
                getCurrentWarehouseId();


        if (
                !currentWarehouseId
                        .equals(warehouseId)
        ) {

            throw new Forbidden(
                    "You cannot access locations from another warehouse"
            );
        }
    }


    /*
     * =====================================================
     * VALIDATE LOCATION WAREHOUSE
     * =====================================================
     */

    private void validateWarehouse(
            WarehouseLocations location,
            Long warehouseId
    ) {

        if (
                location.getWarehouse() == null
                        ||
                        !location
                                .getWarehouse()
                                .getId()
                                .equals(warehouseId)
        ) {

            throw new Forbidden(
                    "Warehouse location belongs to another warehouse"
            );
        }
    }


    /*
     * =====================================================
     * VALIDATE RECEIVING
     * =====================================================
     */

    private void validateNoOtherActiveReceiving(
            Long warehouseId,
            Long currentLocationId
    ) {

        WarehouseLocations receiving =
                warehouseLocationRepository
                        .findFirstByWarehouseIdAndLocationTypeAndStatus(
                                warehouseId,
                                WarehouseLocationType.RECEIVING,
                                WarehouseStatus.ACTIVE
                        )
                        .orElse(null);


        if (receiving == null) {
            return;
        }


        if (
                currentLocationId != null
                        &&
                        receiving
                                .getId()
                                .equals(currentLocationId)
        ) {
            return;
        }


        throw new BadRequest(
                "An active receiving location already exists: "
                        + receiving.getWarehouseCode()
        );
    }


    /*
     * =====================================================
     * NORMALIZE REQUIRED CODE
     * =====================================================
     */

    private String normalizeRequiredCode(
            String value
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            throw new BadRequest(
                    "Warehouse location code is required"
            );
        }


        return value
                .trim()
                .toUpperCase();
    }


    /*
     * =====================================================
     * NORMALIZE OPTIONAL CODE
     * =====================================================
     */

    private String normalizeNullableCode(
            String value
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            return null;
        }


        return value
                .trim()
                .toUpperCase();
    }


    /*
     * =====================================================
     * INTERNAL LOCATION CODE
     * =====================================================
     */

    private String generateInternalLocationCode() {

        return "WL-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}