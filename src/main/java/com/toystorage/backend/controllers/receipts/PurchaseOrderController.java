package com.toystorage.backend.controllers.receipts;

import com.toystorage.backend.dto.request.receipts.CancelPurchaseOrderRequest;
import com.toystorage.backend.dto.request.receipts.CreatePurchaseOrderRequest;
import com.toystorage.backend.dto.request.receipts.UpdatePurchaseOrderRequest;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderDetailResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderOptionResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderProductOptionResponse;
import com.toystorage.backend.dto.response.receipts.PurchaseOrderSummaryResponse;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.services.receipts.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;


    // =====================================================
    // GET PURCHASE ORDER LIST
    // =====================================================

    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<PurchaseOrderSummaryResponse>>
    getPurchaseOrders(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            PurchaseOrderStatus status,

            @RequestParam(required = false)
            Long supplierId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdTo
    ) {

        return ResponseEntity.ok(
                purchaseOrderService.getPurchaseOrders(
                        keyword,
                        status,
                        supplierId,
                        createdFrom,
                        createdTo
                )
        );
    }


    // =====================================================
    // GET PURCHASE ORDER DETAIL
    // =====================================================

    @GetMapping("/{orderId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<PurchaseOrderDetailResponse>
    getPurchaseOrderDetail(
            @PathVariable Long orderId
    ) {

        return ResponseEntity.ok(
                purchaseOrderService
                        .getPurchaseOrderDetail(
                                orderId
                        )
        );
    }


    // =====================================================
    // CREATE PURCHASE ORDER
    // =====================================================

    @PostMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<PurchaseOrderDetailResponse>
    createPurchaseOrder(

            @Valid
            @RequestBody
            CreatePurchaseOrderRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        purchaseOrderService.create(
                                request
                        )
                );
    }


    // =====================================================
    // UPDATE DRAFT PURCHASE ORDER
    // =====================================================

    @PutMapping("/{orderId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_UPDATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<PurchaseOrderDetailResponse>
    updatePurchaseOrder(

            @PathVariable
            Long orderId,

            @Valid
            @RequestBody
            UpdatePurchaseOrderRequest request
    ) {

        return ResponseEntity.ok(
                purchaseOrderService.update(
                        orderId,
                        request
                )
        );
    }


    // =====================================================
    // SUBMIT PURCHASE ORDER
    // DRAFT -> ORDERED
    // =====================================================

    @PatchMapping("/{orderId}/submit")
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_SUBMIT',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<PurchaseOrderDetailResponse>
    submitPurchaseOrder(

            @PathVariable
            Long orderId
    ) {

        return ResponseEntity.ok(
                purchaseOrderService.submit(
                        orderId
                )
        );
    }


    // =====================================================
    // CANCEL PURCHASE ORDER
    // =====================================================

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_CANCEL',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<PurchaseOrderDetailResponse>
    cancelPurchaseOrder(

            @PathVariable
            Long orderId,

            @Valid
            @RequestBody
            CancelPurchaseOrderRequest request
    ) {

        return ResponseEntity.ok(
                purchaseOrderService.cancel(
                        orderId,
                        request
                )
        );
    }


    // =====================================================
    // GET SUPPLIER OPTIONS
    // =====================================================

    @GetMapping("/options/suppliers")
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_VIEW',
                'PURCHASE_ORDER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<PurchaseOrderOptionResponse>>
    getSupplierOptions() {

        return ResponseEntity.ok(
                purchaseOrderService
                        .getSupplierOptions()
        );
    }


    // =====================================================
    // GET MAIN WAREHOUSE OPTIONS
    // =====================================================

    @GetMapping("/options/main-warehouses")
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_VIEW',
                'PURCHASE_ORDER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<PurchaseOrderOptionResponse>>
    getMainWarehouseOptions() {

        return ResponseEntity.ok(
                purchaseOrderService
                        .getMainWarehouseOptions()
        );
    }


    // =====================================================
    // GET PRODUCTS LINKED TO SUPPLIER
    // =====================================================

    @GetMapping(
            "/options/suppliers/{supplierId}/products"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'PURCHASE_ORDER_VIEW',
                'PURCHASE_ORDER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<
            List<PurchaseOrderProductOptionResponse>
            >
    getProductsBySupplier(

            @PathVariable
            Long supplierId
    ) {

        return ResponseEntity.ok(
                purchaseOrderService
                        .getProductsBySupplier(
                                supplierId
                        )
        );
    }
}