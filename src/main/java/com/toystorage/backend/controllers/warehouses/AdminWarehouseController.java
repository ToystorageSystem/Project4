package com.toystorage.backend.controllers.warehouses;

import com.toystorage.backend.dto.request.warehouses.CreateWarehouseRequest;
import com.toystorage.backend.dto.request.warehouses.UpdateWarehouseRequest;

import com.toystorage.backend.dto.response.warehouses.WarehouseDeactivationImpactResponse;
import com.toystorage.backend.dto.response.warehouses.WarehouseEmployeeResponse;
import com.toystorage.backend.dto.response.warehouses.WarehouseResponse;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;

import com.toystorage.backend.services.warehouses.WarehouseAssignmentService;
import com.toystorage.backend.services.warehouses.WarehouseImpactService;
import com.toystorage.backend.services.warehouses.WarehouseManagementService;
import com.toystorage.backend.services.warehouses.WarehouseQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/warehouses")
@RequiredArgsConstructor
public class AdminWarehouseController {

    private final WarehouseQueryService warehouseQueryService;

    private final WarehouseManagementService warehouseManagementService;

    private final WarehouseImpactService warehouseImpactService;

    private final WarehouseAssignmentService warehouseAssignmentService;


    // =========================================================
    // LIST + SEARCH + FILTER
    // =========================================================

    @GetMapping
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_READ')"
    )
    public ResponseEntity<Page<WarehouseResponse>>
    getWarehouses(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WarehouseType type,

            @RequestParam(required = false)
            WarehouseStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.ASC,
                                "warehousesCode"
                        )
                );


        Page<WarehouseResponse> result =
                warehouseQueryService
                        .getWarehouses(
                                keyword,
                                type,
                                status,
                                pageable
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // SELECTABLE ACTIVE LOCATIONS
    //
    // Only ACTIVE warehouses/stores are returned.
    //
    // Examples:
    //
    // GET /api/admin/warehouses/selectable
    //
    // GET /api/admin/warehouses/selectable?type=STORE
    //
    // GET /api/admin/warehouses/selectable?type=MAIN_WAREHOUSE
    // =========================================================

    @GetMapping("/selectable")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_READ')"
    )
    public ResponseEntity<List<WarehouseResponse>>
    getSelectableWarehouses(

            @RequestParam(required = false)
            WarehouseType type
    ) {

        List<WarehouseResponse> result =
                warehouseQueryService
                        .getSelectableWarehouses(
                                type
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // DETAIL
    // =========================================================

    @GetMapping("/{warehouseId}")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_READ')"
    )
    public ResponseEntity<WarehouseResponse>
    getWarehouseById(

            @PathVariable
            Long warehouseId
    ) {

        return warehouseQueryService
                .getWarehouseById(
                        warehouseId
                )
                .map(
                        ResponseEntity::ok
                )
                .orElseGet(
                        () ->
                                ResponseEntity
                                        .notFound()
                                        .build()
                );
    }


    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseResponse>
    createWarehouse(

            @Valid
            @RequestBody
            CreateWarehouseRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseResponse result =
                warehouseManagementService
                        .createWarehouse(
                                request,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        result
                );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{warehouseId}")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseResponse>
    updateWarehouse(

            @PathVariable
            Long warehouseId,

            @Valid
            @RequestBody
            UpdateWarehouseRequest request,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseResponse result =
                warehouseManagementService
                        .updateWarehouse(
                                warehouseId,
                                request,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // DEACTIVATION IMPACT
    // =========================================================

    @GetMapping("/{warehouseId}/deactivation-impact")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_READ')"
    )
    public ResponseEntity<WarehouseDeactivationImpactResponse>
    getDeactivationImpact(

            @PathVariable
            Long warehouseId
    ) {

        WarehouseDeactivationImpactResponse result =
                warehouseImpactService
                        .getDeactivationImpact(
                                warehouseId
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // DEACTIVATE
    // ACTIVE -> INACTIVE
    // =========================================================

    @PatchMapping("/{warehouseId}/inactive")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseResponse>
    deactivateWarehouse(

            @PathVariable
            Long warehouseId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseResponse result =
                warehouseManagementService
                        .deactivateWarehouse(
                                warehouseId,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // ACTIVATE
    // INACTIVE -> ACTIVE
    // =========================================================

    @PatchMapping("/{warehouseId}/active")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseResponse>
    activateWarehouse(

            @PathVariable
            Long warehouseId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseResponse result =
                warehouseManagementService
                        .activateWarehouse(
                                warehouseId,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // LIST EMPLOYEES ASSIGNED TO LOCATION
    // =========================================================

    @GetMapping("/{warehouseId}/employees")
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_READ')"
    )
    public ResponseEntity<List<WarehouseEmployeeResponse>>
    getEmployees(

            @PathVariable
            Long warehouseId
    ) {

        List<WarehouseEmployeeResponse> result =
                warehouseAssignmentService
                        .getEmployees(
                                warehouseId
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // ASSIGN EMPLOYEE
    // =========================================================

    @PatchMapping(
            "/{warehouseId}/employees/{userId}/assign"
    )
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseEmployeeResponse>
    assignEmployee(

            @PathVariable
            Long warehouseId,

            @PathVariable
            Long userId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseEmployeeResponse result =
                warehouseAssignmentService
                        .assignEmployee(
                                warehouseId,
                                userId,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // UNASSIGN EMPLOYEE
    // =========================================================

    @PatchMapping(
            "/{warehouseId}/employees/{userId}/unassign"
    )
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseEmployeeResponse>
    unassignEmployee(

            @PathVariable
            Long warehouseId,

            @PathVariable
            Long userId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseEmployeeResponse result =
                warehouseAssignmentService
                        .unassignEmployee(
                                warehouseId,
                                userId,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }


    // =========================================================
    // ASSIGN MANAGER
    // =========================================================

    @PatchMapping(
            "/{warehouseId}/manager/{userId}"
    )
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('WAREHOUSE_MANAGE')"
    )
    public ResponseEntity<WarehouseEmployeeResponse>
    assignManager(

            @PathVariable
            Long warehouseId,

            @PathVariable
            Long userId,

            Authentication authentication,

            HttpServletRequest httpRequest
    ) {

        WarehouseEmployeeResponse result =
                warehouseAssignmentService
                        .assignManager(
                                warehouseId,
                                userId,
                                authentication,
                                httpRequest
                        );


        return ResponseEntity.ok(
                result
        );
    }
}