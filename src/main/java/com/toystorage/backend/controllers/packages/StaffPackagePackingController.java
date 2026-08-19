package com.toystorage.backend.controllers.packages;

import com.toystorage.backend.dto.request.packages.ScanPackageItemRequest;
import com.toystorage.backend.dto.request.packages.SealPackageRequest;

import com.toystorage.backend.dto.response.packages.StaffPackingPackageResponse;

import com.toystorage.backend.services.packages.StaffPackagePackingService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/warehouse/staff/packing"
)
@RequiredArgsConstructor
public class StaffPackagePackingController {

    private final StaffPackagePackingService
            packingService;


    // =====================================================
    // CREATE NEW PACKAGE
    // =====================================================

    @PostMapping(
            "/transfers/{transferId}/packages"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<StaffPackingPackageResponse>
    createPackage(
            @PathVariable
            Long transferId
    ) {

        return ResponseEntity.ok(
                packingService
                        .createPackage(
                                transferId
                        )
        );
    }


    // =====================================================
    // SCAN PRODUCT INTO PACKAGE
    // =====================================================

    @PostMapping(
            "/packages/{packageId}/scan"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<StaffPackingPackageResponse>
    scanItem(
            @PathVariable
            Long packageId,

            @Valid
            @RequestBody
            ScanPackageItemRequest request
    ) {

        return ResponseEntity.ok(
                packingService
                        .scanItem(
                                packageId,
                                request
                        )
        );
    }


    // =====================================================
    // VIEW PACKAGE
    // =====================================================

    @GetMapping(
            "/packages/{packageId}"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<StaffPackingPackageResponse>
    getPackage(
            @PathVariable
            Long packageId
    ) {

        return ResponseEntity.ok(
                packingService
                        .getPackage(
                                packageId
                        )
        );
    }


    // =====================================================
    // SEAL PACKAGE
    // =====================================================

    @PatchMapping(
            "/packages/{packageId}/seal"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<StaffPackingPackageResponse>
    sealPackage(
            @PathVariable
            Long packageId,

            @Valid
            @RequestBody
            SealPackageRequest request
    ) {

        return ResponseEntity.ok(
                packingService
                        .sealPackage(
                                packageId,
                                request
                        )
        );
    }


    // =====================================================
    // COMPLETE TRANSFER PACKING
    // =====================================================

    @PatchMapping(
            "/transfers/{transferId}/complete"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<Void>
    completePacking(
            @PathVariable
            Long transferId
    ) {

        packingService
                .completePacking(
                        transferId
                );

        return ResponseEntity
                .noContent()
                .build();
    }
}