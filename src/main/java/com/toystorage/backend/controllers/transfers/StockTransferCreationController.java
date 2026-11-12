package com.toystorage.backend.controllers.transfers;


import com.toystorage.backend.dto.request.transfers.picking.CancelStockTransferRequest;
import com.toystorage.backend.dto.request.transfers.picking.CreateStockTransferRequest;
import com.toystorage.backend.dto.request.transfers.picking.UpdateStockTransferRequest;

import com.toystorage.backend.dto.response.transfers.delivery.StockTransferDetailResponse;
import com.toystorage.backend.dto.response.transfers.delivery.StockTransferListResponse;
import com.toystorage.backend.dto.response.transfers.delivery.StockTransferProductOptionResponse;
import com.toystorage.backend.dto.response.transfers.delivery.StockTransferWarehouseOptionResponse;

import com.toystorage.backend.services.transfers.business.BusinessStaffTransferListService;
import com.toystorage.backend.services.transfers.business.StockTransferCreationService;
import com.toystorage.backend.enums.transfers.TransferStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
public class StockTransferCreationController {

    private final StockTransferCreationService
            stockTransferCreationService;
    private final BusinessStaffTransferListService
        businessStaffTransferListService;


    // =====================================================
    // GET SOURCE OPTIONS
    // MAIN_WAREHOUSE + STORE
    // =====================================================

    @GetMapping("/creation-options/sources")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<List<StockTransferWarehouseOptionResponse>>
    getSourceOptions() {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .getSourceOptions()
        );
    }


    // =====================================================
    // GET DESTINATION OPTIONS
    // STORE ONLY
    // =====================================================

    @GetMapping("/creation-options/destinations")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<List<StockTransferWarehouseOptionResponse>>
    getDestinationOptions() {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .getDestinationOptions()
        );
    }


    // =====================================================
    // GET PRODUCTS + AVAILABLE INVENTORY AT SOURCE
    // =====================================================

    @GetMapping(
            "/creation-options/sources/{sourceId}/products"
    )
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<List<StockTransferProductOptionResponse>>
    getProductsBySource(

            @PathVariable
            Long sourceId
    ) {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .getProductsBySource(
                                sourceId
                        )
        );
    }


    // =====================================================
    // CREATE STOCK TRANSFER DRAFT
    // =====================================================

    @PostMapping
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<StockTransferDetailResponse>
    createStockTransfer(

            @Valid
            @RequestBody
            CreateStockTransferRequest request
    ) {

        StockTransferDetailResponse response =
                stockTransferCreationService
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
    // BUSINESS STAFF - VIEW MY STOCK TRANSFERS
    // GET /api/stock-transfers/mine
    // =====================================================

    @GetMapping("/mine")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<Page<StockTransferListResponse>>
    getMyStockTransfers(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            TransferStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        return ResponseEntity.ok(
                businessStaffTransferListService.getMyTransfers(
                        keyword,
                        status,
                        page,
                        size
                )
        );
    }
    // =====================================================
    // GET STOCK TRANSFER DETAIL
    // =====================================================

    @GetMapping("/{transferId}")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<StockTransferDetailResponse>
    getStockTransferDetail(

            @PathVariable
            Long transferId
    ) {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .getDetail(
                                transferId
                        )
        );
    }


    // =====================================================
    // SUBMIT STOCK TRANSFER
    // DRAFT -> CREATED
    // =====================================================

    @PatchMapping("/{transferId}/submit")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<StockTransferDetailResponse>
    submitStockTransfer(

            @PathVariable
            Long transferId
    ) {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .submit(
                                transferId
                        )
        );
    }


    // =====================================================
    // UPDATE STOCK TRANSFER
    // =====================================================

    @PutMapping("/{transferId}")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<StockTransferDetailResponse>
    updateStockTransfer(

            @PathVariable
            Long transferId,

            @Valid
            @RequestBody
            UpdateStockTransferRequest request
    ) {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .update(
                                transferId,
                                request
                        )
        );
    }


    // =====================================================
    // CANCEL STOCK TRANSFER
    // =====================================================

    @PatchMapping("/{transferId}/cancel")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'STOCK_TRANSFER_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<StockTransferDetailResponse>
    cancelStockTransfer(

            @PathVariable
            Long transferId,

            @Valid
            @RequestBody
            CancelStockTransferRequest request
    ) {

        return ResponseEntity.ok(
                stockTransferCreationService
                        .cancel(
                                transferId,
                                request
                        )
        );
    }
}