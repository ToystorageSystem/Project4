package com.toystorage.backend.controllers.receipts;

import com.toystorage.backend.dto.request.receipts.receiving.CreateGoodsReceiptRequest;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptDetailResponse;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptOptionResponse;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptSummaryResponse;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptTrackingPageResponse;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.services.receipts.receiving.GoodsReceiptCreationService;
import com.toystorage.backend.services.receipts.receiving.GoodsReceiptTrackingService;
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
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptCreationController {

    private final GoodsReceiptCreationService goodsReceiptCreationService;
    private final GoodsReceiptTrackingService goodsReceiptTrackingService;

    // BUSINESS STAFF - Theo doi phieu nhap, tim theo GR/PO va loc trang thai.
    // Khong gioi han "cua toi" vi goods_receipts chua luu created_by.
    @GetMapping("/business")
    @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<GoodsReceiptTrackingPageResponse> getBusinessGoodsReceipts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(goodsReceiptTrackingService.search(
                keyword, purchaseOrderId, warehouseId, status, fromDate, toDate, page, size));
    }


    // =====================================================
    // BUSINESS STAFF CREATE GOODS RECEIPT
    // =====================================================

    @PostMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'GOODS_RECEIPT_CREATE',
                'ROLE_ROLE-BIZ-OPS',
                'ROLE_ROLE-ADMIN'
            )
            """)
    public ResponseEntity<GoodsReceiptDetailResponse> createGoodsReceipt(
            @Valid
            @RequestBody CreateGoodsReceiptRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        goodsReceiptCreationService
                                .create(request)
                );
    }


    // =====================================================
    // SUPPLIER OPTIONS FOR CREATE FORM
    // =====================================================

    @GetMapping("/creation-options/suppliers")
    @PreAuthorize("""
            hasAnyAuthority(
                'GOODS_RECEIPT_CREATE',
                'ROLE_ROLE-BIZ-OPS',
                'ROLE_ROLE-ADMIN'
            )
            """)
    public ResponseEntity<List<GoodsReceiptOptionResponse>>
    getSupplierOptions() {

        return ResponseEntity.ok(
                goodsReceiptCreationService
                        .getActiveSuppliers()
        );
    }


    // =====================================================
    // MAIN WAREHOUSE OPTIONS FOR CREATE FORM
    // =====================================================

    @GetMapping("/creation-options/main-warehouses")
    @PreAuthorize("""
            hasAnyAuthority(
                'GOODS_RECEIPT_CREATE',
                'ROLE_ROLE-BIZ-OPS',
                'ROLE_ROLE-ADMIN'
            )
            """)
    public ResponseEntity<List<GoodsReceiptOptionResponse>>
    getMainWarehouseOptions() {

        return ResponseEntity.ok(
                goodsReceiptCreationService
                        .getActiveMainWarehouses()
        );
    }


    // =====================================================
    // WAREHOUSE RECEIPT LIST
    // =====================================================

    @GetMapping("/warehouse")
    @PreAuthorize("""
            hasAnyAuthority(
                'GOODS_RECEIPT_VIEW',
                'ROLE_WH_MANAGER',
                'ROLE_STAFF',
                'ROLE_ROLE-ADMIN'
            )
            """)
    public ResponseEntity<List<GoodsReceiptSummaryResponse>>
    getWarehouseReceipts(
            @RequestParam(required = false)
            GoodsReceiptStatus status
    ) {

        return ResponseEntity.ok(
                goodsReceiptCreationService
                        .getCurrentWarehouseReceipts(
                                status
                        )
        );
    }


    // =====================================================
    // GOODS RECEIPT DETAIL
    // =====================================================

    @GetMapping("/{receiptId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'GOODS_RECEIPT_VIEW',
                'ROLE_ROLE-BIZ-OPS',
                'ROLE_WH_MANAGER',
                'ROLE_STAFF',
                'ROLE_ROLE-ADMIN'
            )
            """)
    public ResponseEntity<GoodsReceiptDetailResponse>
    getGoodsReceipt(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                goodsReceiptCreationService
                        .getById(receiptId)
        );
    }
}