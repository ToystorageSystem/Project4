package com.toystorage.backend.controllers.receipts;

import com.toystorage.backend.dto.request.receipts.CreateGoodsReceiptRequest;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptDetailResponse;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptOptionResponse;
import com.toystorage.backend.dto.response.receipts.GoodsReceiptSummaryResponse;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import com.toystorage.backend.services.receipts.GoodsReceiptCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptCreationController {

    private final GoodsReceiptCreationService goodsReceiptCreationService;


    // =====================================================
    // BUSINESS STAFF CREATE GOODS RECEIPT
    // =====================================================

    @PostMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'GOODS_RECEIPT_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_ADMIN'
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
                'ROLE_BUSINESS_STAFF',
                'ROLE_ADMIN'
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
                'ROLE_BUSINESS_STAFF',
                'ROLE_ADMIN'
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
                'ROLE_ADMIN'
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
                'ROLE_BUSINESS_STAFF',
                'ROLE_WH_MANAGER',
                'ROLE_STAFF',
                'ROLE_ADMIN'
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