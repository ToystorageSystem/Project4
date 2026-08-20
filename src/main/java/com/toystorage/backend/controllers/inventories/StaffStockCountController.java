package com.toystorage.backend.controllers.inventories;

import com.toystorage.backend.dto.request.inventories.StaffStockCountItemRequest;

import com.toystorage.backend.dto.response.inventories.StaffStockCountDetailResponse;
import com.toystorage.backend.dto.response.inventories.StaffStockCountListResponse;

import com.toystorage.backend.services.inventories.StaffStockCountService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/warehouse/staff/stock-counts"
)
@RequiredArgsConstructor
public class StaffStockCountController {

    private final StaffStockCountService
            stockCountService;


    // =====================================================
    // AVAILABLE + MY TASKS
    // =====================================================

    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_VIEW')"
//    )
    public ResponseEntity<List<StaffStockCountListResponse>>
    getTasks() {

        return ResponseEntity.ok(
                stockCountService
                        .getTasks()
        );
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @GetMapping(
            "/{stockCountId}"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_VIEW')"
//    )
    public ResponseEntity<StaffStockCountDetailResponse>
    getDetail(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .getDetail(
                                stockCountId
                        )
        );
    }


    // =====================================================
    // CLAIM + START
    // =====================================================

    @PatchMapping(
            "/{stockCountId}/start"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_EXECUTE')"
//    )
    public ResponseEntity<StaffStockCountDetailResponse>
    start(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .start(
                                stockCountId
                        )
        );
    }


    // =====================================================
    // COUNT / RECOUNT
    // =====================================================

    @PutMapping(
            "/{stockCountId}/items"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_EXECUTE')"
//    )
    public ResponseEntity<StaffStockCountDetailResponse>
    countItem(
            @PathVariable Long stockCountId,

            @Valid
            @RequestBody
            StaffStockCountItemRequest request
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .countItem(
                                stockCountId,
                                request
                        )
        );
    }


    // =====================================================
    // SUBMIT TO WAREHOUSE MANAGER
    // =====================================================

    @PatchMapping(
            "/{stockCountId}/submit"
    )
//    @PreAuthorize(
//            "hasAuthority('STOCK_COUNT_EXECUTE')"
//    )
    public ResponseEntity<StaffStockCountDetailResponse>
    submit(
            @PathVariable Long stockCountId
    ) {

        return ResponseEntity.ok(
                stockCountService
                        .submit(
                                stockCountId
                        )
        );
    }
}