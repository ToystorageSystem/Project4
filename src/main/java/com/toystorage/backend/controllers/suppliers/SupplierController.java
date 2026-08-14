package com.toystorage.backend.controllers.suppliers;

import com.toystorage.backend.config.security.CustomUserDetails;
import com.toystorage.backend.dto.request.suppliers.CreateSupplierRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierPageResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierResponse;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.services.suppliers.SupplierService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Validated
public class SupplierController {

    private final SupplierService
            supplierService;

    /*
     * DANH SÁCH, TÌM KIẾM, LỌC VÀ PHÂN TRANG
     */
    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_VIEW',
                'ROLE_ADMIN',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_BUSINESS_STAFF'
            )
            """)
    public ResponseEntity<SupplierPageResponse>
    getSuppliers(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            CommonStatus status,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            Sort.Direction direction
    ) {

        return ResponseEntity.ok(
                supplierService.getSuppliers(
                        keyword,
                        status,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    /*
     * XEM CHI TIẾT NHÀ CUNG CẤP
     */
    @GetMapping("/{supplierId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_VIEW',
                'ROLE_ADMIN',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_BUSINESS_STAFF'
            )
            """)
    public ResponseEntity<SupplierResponse>
    getSupplier(

            @PathVariable
            Long supplierId
    ) {

        return ResponseEntity.ok(
                supplierService.getSupplier(
                        supplierId
                )
        );
    }

    /*
     * TẠO NHÀ CUNG CẤP
     */
    @PostMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_CREATE',
                'SUPPLIER_MANAGE',
                'ROLE_ADMIN',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_BUSINESS_STAFF'
            )
            """)
    public ResponseEntity<SupplierResponse>
    createSupplier(

            @Valid
            @RequestBody
            CreateSupplierRequest request,

            @AuthenticationPrincipal
            CustomUserDetails principal,

            HttpServletRequest servletRequest
    ) {

        SupplierResponse response =
                supplierService.createSupplier(
                        request,
                        principal.getUser(),
                        servletRequest.getRemoteAddr(),
                        servletRequest.getHeader(
                                "User-Agent"
                        )
                );

        URI location =
                ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(
                                response.getId()
                        )
                        .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /*
     * CẬP NHẬT NHÀ CUNG CẤP
     */
    @PutMapping("/{supplierId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_UPDATE',
                'SUPPLIER_MANAGE',
                'ROLE_ADMIN',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_BUSINESS_STAFF'
            )
            """)
    public ResponseEntity<SupplierResponse>
    updateSupplier(

            @PathVariable
            Long supplierId,

            @Valid
            @RequestBody
            UpdateSupplierRequest request,

            @AuthenticationPrincipal
            CustomUserDetails principal,

            HttpServletRequest servletRequest
    ) {

        return ResponseEntity.ok(
                supplierService.updateSupplier(
                        supplierId,
                        request,
                        principal.getUser(),
                        servletRequest.getRemoteAddr(),
                        servletRequest.getHeader(
                                "User-Agent"
                        )
                )
        );
    }

    /*
     * ẨN NHÀ CUNG CẤP
     */
    @PatchMapping("/{supplierId}/deactivate")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_STATUS_UPDATE',
                'SUPPLIER_MANAGE',
                'ROLE_ADMIN',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_BUSINESS_STAFF'
            )
            """)
    public ResponseEntity<SupplierResponse>
    deactivateSupplier(

            @PathVariable
            Long supplierId,

            @AuthenticationPrincipal
            CustomUserDetails principal,

            HttpServletRequest servletRequest
    ) {

        return ResponseEntity.ok(
                supplierService.deactivateSupplier(
                        supplierId,
                        principal.getUser(),
                        servletRequest.getRemoteAddr(),
                        servletRequest.getHeader(
                                "User-Agent"
                        )
                )
        );
    }

    /*
     * KHÔI PHỤC NHÀ CUNG CẤP
     */
    @PatchMapping("/{supplierId}/activate")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_STATUS_UPDATE',
                'SUPPLIER_MANAGE',
                'ROLE_ADMIN',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_BUSINESS_STAFF'
            )
            """)
    public ResponseEntity<SupplierResponse>
    activateSupplier(

            @PathVariable
            Long supplierId,

            @AuthenticationPrincipal
            CustomUserDetails principal,

            HttpServletRequest servletRequest
    ) {

        return ResponseEntity.ok(
                supplierService.activateSupplier(
                        supplierId,
                        principal.getUser(),
                        servletRequest.getRemoteAddr(),
                        servletRequest.getHeader(
                                "User-Agent"
                        )
                )
        );
    }
}