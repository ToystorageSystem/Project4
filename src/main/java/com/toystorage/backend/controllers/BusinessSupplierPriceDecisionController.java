
package com.toystorage.backend.controllers.businessstaff;

import com.toystorage.backend.dto.request.businessstaff.SupplierPriceDecisionRequest;
import com.toystorage.backend.dto.response.businessstaff.SupplierPriceDecisionResponse;
import com.toystorage.backend.services.businessstaff.BusinessSupplierPriceDecisionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-staff/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyAuthority('ROLE_ROLE-BIZ-STAFF', "
        + "'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-ADMIN')"
)
public class BusinessSupplierPriceDecisionController {

    private final BusinessSupplierPriceDecisionService decisionService;

    @PostMapping("/{purchaseOrderId}/items/{purchaseOrderItemId}/price-decision")
    public ResponseEntity<SupplierPriceDecisionResponse> decidePrice(
            @PathVariable Long purchaseOrderId,
            @PathVariable Long purchaseOrderItemId,
            @Valid @RequestBody SupplierPriceDecisionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                decisionService.decidePrice(
                        purchaseOrderId,
                        purchaseOrderItemId,
                        request,
                        authentication
                )
        );
    }
}