package com.toystorage.backend.controllers.transfers;

import com.toystorage.backend.dto.request.transfers.CreateStockTransferRequest;
import com.toystorage.backend.dto.response.transfers.StockTransferDetailResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferProductOptionResponse;
import com.toystorage.backend.dto.response.transfers.StockTransferWarehouseOptionResponse;
import com.toystorage.backend.services.transfers.StockTransferCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
public class StockTransferCreationController {

    private final StockTransferCreationService
            stockTransferCreationService;


    // =====================================================
    // GET SOURCE OPTIONS
    // MAIN_WAREHOUSE + STORE
    // =====================================================

    @GetMapping("/creation-options/sources")
    @PreAuthorize("""
            hasAnyAuthority(
                'STOCK_TRANSFER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
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
    @PreAuthorize("""
            hasAnyAuthority(
                'STOCK_TRANSFER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
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
    @PreAuthorize("""
            hasAnyAuthority(
                'STOCK_TRANSFER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
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
    @PreAuthorize("""
            hasAnyAuthority(
                'STOCK_TRANSFER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
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
    // GET STOCK TRANSFER DETAIL
    // =====================================================

    @GetMapping("/{transferId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'STOCK_TRANSFER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
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
    @PreAuthorize("""
            hasAnyAuthority(
                'STOCK_TRANSFER_CREATE',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
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
}