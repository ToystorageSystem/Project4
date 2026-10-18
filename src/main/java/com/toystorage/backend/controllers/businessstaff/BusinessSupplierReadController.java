
package com.toystorage.backend.controllers.businessstaff;

import com.toystorage.backend.dto.response.businessstaff.SupplierInfoResponse;
import com.toystorage.backend.dto.response.businessstaff.SupplierProductResponse;
import com.toystorage.backend.services.businessstaff.BusinessSupplierReadService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business-staff/suppliers")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAnyAuthority('ROLE_ROLE-BIZ-STAFF', " +
    "'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-ADMIN')"
)
public class BusinessSupplierReadController {

    private final BusinessSupplierReadService supplierReadService;

    @GetMapping
    public ResponseEntity<List<SupplierInfoResponse>> getSuppliers() {
        return ResponseEntity.ok(
                supplierReadService.getSuppliers()
        );
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierInfoResponse> getSupplier(
            @PathVariable Long supplierId
    ) {
        return ResponseEntity.ok(
                supplierReadService.getSupplier(supplierId)
        );
    }

    @GetMapping("/{supplierId}/products")
    public ResponseEntity<List<SupplierProductResponse>>
    getSupplierProducts(@PathVariable Long supplierId) {
        return ResponseEntity.ok(
                supplierReadService.getSupplierProducts(supplierId)
        );
    }
}
