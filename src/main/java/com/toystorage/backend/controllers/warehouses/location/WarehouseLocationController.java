package com.toystorage.backend.controllers.warehouses.location;

import com.toystorage.backend.dto.request.warehouses.location.CreateWarehouseLocationRequest;
import com.toystorage.backend.dto.request.warehouses.location.UpdateWarehouseLocationRequest;
import com.toystorage.backend.dto.request.warehouses.location.UpdateWarehouseLocationStatusRequest;

import com.toystorage.backend.dto.response.warehouses.location.WarehouseLocationResponse;

import com.toystorage.backend.services.warehouses.location.WarehouseLocationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/locations")
@RequiredArgsConstructor
public class WarehouseLocationController {

    private final WarehouseLocationService
            warehouseLocationService;


    /*
     * =====================================================
     * GET ALL
     * =====================================================
     *
     * GET /api/warehouse/locations
     */

    @GetMapping
    public ResponseEntity<List<WarehouseLocationResponse>>
    getLocations() {

        return ResponseEntity.ok(
                warehouseLocationService
                        .getLocations()
        );
    }


    /*
     * =====================================================
     * RECEIVING LOCATION
     * =====================================================
     *
     * GET
     * /api/warehouse/locations/receiving?warehouseId=9
     */

    @GetMapping("/receiving")
    public ResponseEntity<WarehouseLocationResponse>
    getReceivingLocation(
            @RequestParam Long warehouseId
    ) {

        return ResponseEntity.ok(
                warehouseLocationService
                        .getReceivingLocationResponse(
                                warehouseId
                        )
        );
    }


    /*
     * =====================================================
     * FIND BY CODE
     * =====================================================
     *
     * GET
     * /api/warehouse/locations/by-code
     * ?warehouseId=9
     * &locationCode=A01
     */

    @GetMapping("/by-code")
    public ResponseEntity<WarehouseLocationResponse>
    getLocationByCode(
            @RequestParam Long warehouseId,
            @RequestParam String locationCode
    ) {

        return ResponseEntity.ok(
                warehouseLocationService
                        .getLocationByCode(
                                warehouseId,
                                locationCode
                        )
        );
    }


    /*
     * =====================================================
     * GET DETAIL
     * =====================================================
     *
     * GET /api/warehouse/locations/12
     */

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseLocationResponse>
    getLocation(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                warehouseLocationService
                        .getLocation(id)
        );
    }


    /*
     * =====================================================
     * CREATE
     * =====================================================
     *
     * POST /api/warehouse/locations
     */

    @PostMapping
    public ResponseEntity<WarehouseLocationResponse>
    createLocation(
            @Valid
            @RequestBody
            CreateWarehouseLocationRequest request
    ) {

        WarehouseLocationResponse response =
                warehouseLocationService
                        .createLocation(
                                request
                        );


        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        response
                );
    }


    /*
     * =====================================================
     * UPDATE
     * =====================================================
     *
     * PUT /api/warehouse/locations/12
     */

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseLocationResponse>
    updateLocation(
            @PathVariable Long id,

            @Valid
            @RequestBody
            UpdateWarehouseLocationRequest request
    ) {

        return ResponseEntity.ok(
                warehouseLocationService
                        .updateLocation(
                                id,
                                request
                        )
        );
    }


    /*
     * =====================================================
     * STATUS
     * =====================================================
     *
     * PATCH /api/warehouse/locations/12/status
     */

    @PatchMapping("/{id}/status")
    public ResponseEntity<WarehouseLocationResponse>
    updateStatus(
            @PathVariable Long id,

            @Valid
            @RequestBody
            UpdateWarehouseLocationStatusRequest request
    ) {

        return ResponseEntity.ok(
                warehouseLocationService
                        .updateStatus(
                                id,
                                request
                        )
        );
    }
}