package com.toystorage.backend.controllers.warehouses;

import com.toystorage.backend.dto.request.inventories.StockCountRequest;
import com.toystorage.backend.dto.response.inventories.StockCountItemResponse;
import com.toystorage.backend.dto.response.inventories.StockCountResponse;
import com.toystorage.backend.services.warehouses.StockCountService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-counts")
@RequiredArgsConstructor
public class StockCountController {

    private final StockCountService
            stockCountService;


    @GetMapping("/{stockCountId}")
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_VIEW')"
//    )
    public ResponseEntity<StockCountResponse>
    getStockCount(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .getStockCount(stockCountId)
        );
    }


    @PatchMapping("/{stockCountId}/start")
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_MANAGE')"
//    )
    public ResponseEntity<StockCountResponse>
    start(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .start(stockCountId)
        );
    }


    @PatchMapping(
            "/{stockCountId}/items/{itemId}/count"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_PERFORM')"
//    )
    public ResponseEntity<StockCountItemResponse>
    count(
            @PathVariable Long stockCountId,
            @PathVariable Long itemId,

            @Valid
            @RequestBody
            StockCountRequest request
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .countProduct(
                                stockCountId,
                                itemId,
                                request
                        )
        );
    }


    @PatchMapping(
            "/{stockCountId}/finish"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_PERFORM')"
//    )
    public ResponseEntity<StockCountResponse>
    finish(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .finishCounting(
                                stockCountId
                        )
        );
    }


    @PatchMapping(
            "/{stockCountId}/confirm"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_MANAGE')"
//    )
    public ResponseEntity<StockCountResponse>
    confirm(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .confirm(stockCountId)
        );
    }
}