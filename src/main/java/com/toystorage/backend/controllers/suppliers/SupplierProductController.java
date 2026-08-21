package com.toystorage.backend.controllers.suppliers;

import com.toystorage.backend.dto.request.suppliers.CreateSupplierProductRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierProductRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierProductResponse;
import com.toystorage.backend.services.suppliers.SupplierProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SupplierProductController {

    private final SupplierProductService supplierProductService;


    // =====================================================
    // GET SUPPLIERS OF PRODUCT
    // =====================================================

    @GetMapping("/products/{productId}/suppliers")
    @PreAuthorize("""
            hasAnyAuthority(
                'PRODUCT_SUPPLIER_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<SupplierProductResponse>>
    getSuppliersByProduct(
            @PathVariable Long productId
    ) {

        return ResponseEntity.ok(
                supplierProductService
                        .getSuppliersByProduct(productId)
        );
    }


    // =====================================================
    // GET PRODUCTS OF SUPPLIER
    // =====================================================

    @GetMapping("/suppliers/{supplierId}/products")
    @PreAuthorize("""
            hasAnyAuthority(
                'PRODUCT_SUPPLIER_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<SupplierProductResponse>>
    getProductsBySupplier(
            @PathVariable Long supplierId
    ) {

        return ResponseEntity.ok(
                supplierProductService
                        .getProductsBySupplier(supplierId)
        );
    }


    // =====================================================
    // CREATE PRODUCT - SUPPLIER LINK
    // =====================================================

    @PostMapping("/supplier-products")
    @PreAuthorize("""
            hasAnyAuthority(
                'PRODUCT_SUPPLIER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierProductResponse>
    createSupplierProduct(
            @Valid
            @RequestBody CreateSupplierProductRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        supplierProductService
                                .create(request)
                );
    }


    // =====================================================
    // UPDATE PRODUCT - SUPPLIER LINK
    // =====================================================

    @PutMapping("/supplier-products/{linkId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'PRODUCT_SUPPLIER_UPDATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierProductResponse>
    updateSupplierProduct(
            @PathVariable Long linkId,

            @Valid
            @RequestBody UpdateSupplierProductRequest request
    ) {

        return ResponseEntity.ok(
                supplierProductService
                        .update(linkId, request)
        );
    }


    // =====================================================
    // SET DEFAULT SUPPLIER
    // =====================================================

    @PatchMapping("/supplier-products/{linkId}/default")
    @PreAuthorize("""
            hasAnyAuthority(
                'PRODUCT_SUPPLIER_UPDATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierProductResponse>
    setDefaultSupplier(
            @PathVariable Long linkId
    ) {

        return ResponseEntity.ok(
                supplierProductService
                        .setDefault(linkId)
        );
    }


    // =====================================================
    // DELETE PRODUCT - SUPPLIER LINK
    // =====================================================

    @DeleteMapping("/supplier-products/{linkId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'PRODUCT_SUPPLIER_DELETE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<Void>
    deleteSupplierProduct(
            @PathVariable Long linkId
    ) {

        supplierProductService.delete(linkId);

        return ResponseEntity.noContent().build();
    }
}