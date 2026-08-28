package com.toystorage.backend.controllers.inventories.discrepancy;

import com.toystorage.backend.dto.response.inventories.discrepancy.BusinessManagerDiscrepancyResponse;
import com.toystorage.backend.services.inventories.discrepancy.BusinessManagerDiscrepancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.toystorage.backend.dto.request.inventories.discrepancy.BusinessManagerResolveDiscrepancyRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/business-manager/discrepancies")
@RequiredArgsConstructor
public class BusinessManagerDiscrepancyController {

    private final BusinessManagerDiscrepancyService
            businessManagerDiscrepancyService;


    // =====================================================
    // BUSINESS MANAGER - LIST
    // =====================================================

    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<List<BusinessManagerDiscrepancyResponse>>
    getReports(

            @RequestParam(required = false)
            String referenceType,

            @RequestParam(required = false)
            String discrepancyType,

            @RequestParam(required = false)
            String status
    ) {

        return ResponseEntity.ok(
                businessManagerDiscrepancyService
                        .getReports(
                                referenceType,
                                discrepancyType,
                                status
                        )
        );
    }


    // =====================================================
    // BUSINESS MANAGER - DETAIL
    // =====================================================

    @GetMapping("/{discrepancyId}")
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<BusinessManagerDiscrepancyResponse>
    getDetail(

            @PathVariable
            Long discrepancyId
    ) {

        return ResponseEntity.ok(
                businessManagerDiscrepancyService
                        .getDetail(
                                discrepancyId
                        )
        );
    }
    // =====================================================
    // BUSINESS MANAGER - RESOLVE
    // =====================================================

    @PatchMapping("/{discrepancyId}/resolve")
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<BusinessManagerDiscrepancyResponse>
    resolve(

            @PathVariable
            Long discrepancyId,

            @Valid
            @RequestBody
            BusinessManagerResolveDiscrepancyRequest request
    ) {

        return ResponseEntity.ok(
                businessManagerDiscrepancyService
                        .resolve(
                                discrepancyId,
                                request
                        )
        );
    }
}