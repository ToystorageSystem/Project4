package com.toystorage.backend.controllers.suppliers;

import com.toystorage.backend.dto.request.suppliers.CancelSupplierInvoiceRequest;
import com.toystorage.backend.dto.request.suppliers.CreateSupplierInvoiceRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierInvoiceRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceDetailResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoicePurchaseOrderOptionResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceSummaryResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierInvoiceSupplierOptionResponse;
import com.toystorage.backend.enums.suppliers.SupplierInvoiceStatus;
import com.toystorage.backend.services.suppliers.SupplierInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/supplier-invoices")
@RequiredArgsConstructor
public class SupplierInvoiceController {

    private final SupplierInvoiceService
            supplierInvoiceService;


    // =====================================================
    // GET SUPPLIER INVOICE LIST
    // =====================================================

    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<SupplierInvoiceSummaryResponse>>
    getSupplierInvoices(

            @RequestParam(
                    required = false
            )
            String keyword,

            @RequestParam(
                    required = false
            )
            SupplierInvoiceStatus status
    ) {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .getSupplierInvoices(
                                keyword,
                                status
                        )
        );
    }


    // =====================================================
    // GET SUPPLIER INVOICE DETAIL
    // =====================================================

    @GetMapping("/{invoiceId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierInvoiceDetailResponse>
    getSupplierInvoiceDetail(

            @PathVariable
            Long invoiceId
    ) {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .getSupplierInvoiceDetail(
                                invoiceId
                        )
        );
    }


    // =====================================================
    // CREATE SUPPLIER INVOICE
    // =====================================================

    @PostMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierInvoiceDetailResponse>
    createSupplierInvoice(

            @Valid
            @RequestBody
            CreateSupplierInvoiceRequest request
    ) {

        SupplierInvoiceDetailResponse response =
                supplierInvoiceService
                        .create(
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


    // =====================================================
    // UPDATE SUPPLIER INVOICE
    // =====================================================

    @PutMapping("/{invoiceId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_UPDATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierInvoiceDetailResponse>
    updateSupplierInvoice(

            @PathVariable
            Long invoiceId,

            @Valid
            @RequestBody
            UpdateSupplierInvoiceRequest request
    ) {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .update(
                                invoiceId,
                                request
                        )
        );
    }


    // =====================================================
    // CANCEL SUPPLIER INVOICE
    // =====================================================

    @PatchMapping("/{invoiceId}/cancel")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_CANCEL',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierInvoiceDetailResponse>
    cancelSupplierInvoice(

            @PathVariable
            Long invoiceId,

            @Valid
            @RequestBody
            CancelSupplierInvoiceRequest request
    ) {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .cancel(
                                invoiceId,
                                request
                        )
        );
    }


    // =====================================================
    // UPLOAD SUPPLIER INVOICE ATTACHMENT
    // =====================================================

    @PostMapping(
            value = "/{invoiceId}/attachment",
            consumes = {
                    "multipart/form-data"
            }
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_UPDATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<SupplierInvoiceDetailResponse>
    uploadAttachment(

            @PathVariable
            Long invoiceId,

            @RequestParam("file")
            MultipartFile file
    ) {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .uploadAttachment(
                                invoiceId,
                                file
                        )
        );
    }


    // =====================================================
    // GET SUPPLIER OPTIONS
    // =====================================================

    @GetMapping("/options/suppliers")
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_VIEW',
                'SUPPLIER_INVOICE_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<
            List<SupplierInvoiceSupplierOptionResponse>
            >
    getSupplierOptions() {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .getSupplierOptions()
        );
    }


    // =====================================================
    // GET PURCHASE ORDER OPTIONS BY SUPPLIER
    // =====================================================

    @GetMapping(
            "/options/suppliers/{supplierId}/purchase-orders"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'SUPPLIER_INVOICE_VIEW',
                'SUPPLIER_INVOICE_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<
            List<SupplierInvoicePurchaseOrderOptionResponse>
            >
    getPurchaseOrderOptions(

            @PathVariable
            Long supplierId
    ) {

        return ResponseEntity.ok(
                supplierInvoiceService
                        .getPurchaseOrderOptions(
                                supplierId
                        )
        );
    }
}