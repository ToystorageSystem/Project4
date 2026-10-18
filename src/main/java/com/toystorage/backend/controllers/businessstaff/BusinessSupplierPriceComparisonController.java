
package com.toystorage.backend.controllers.businessstaff;

import com.toystorage.backend.dto.response.businessstaff.SupplierPriceComparisonResponse;
import com.toystorage.backend.services.businessstaff.BusinessSupplierPriceComparisonService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-staff/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyAuthority('ROLE_ROLE-BIZ-STAFF', "
        + "'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-ADMIN')"
)
public class BusinessSupplierPriceComparisonController {

    private final BusinessSupplierPriceComparisonService comparisonService;

    @GetMapping("/{purchaseOrderId}/price-comparison")
    public ResponseEntity<SupplierPriceComparisonResponse> getPriceComparison(
            @PathVariable Long purchaseOrderId
    ) {
        return ResponseEntity.ok(
                comparisonService.getPriceComparison(purchaseOrderId)
        );
    }
}
