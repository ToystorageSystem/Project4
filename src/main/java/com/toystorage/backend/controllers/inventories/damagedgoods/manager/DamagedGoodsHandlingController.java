package com.toystorage.backend.controllers.inventories.damagedgoods.manager;

import com.toystorage.backend.dto.request.warehouses.damagedgoods.HandleDamagedGoodsRequest;
import com.toystorage.backend.dto.response.warehouses.damagedgoods.DamagedGoodsReportResponse;
import com.toystorage.backend.services.warehouses.damagedgoods.DamagedGoodsHandlingService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/damaged-goods")
@RequiredArgsConstructor
public class DamagedGoodsHandlingController {

    private final DamagedGoodsHandlingService
            damagedGoodsHandlingService;


    // =====================================================
    // LIST ACTIVE DAMAGED GOODS REPORTS
    // =====================================================

    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_VIEW')"
//    )
    public ResponseEntity<
            List<DamagedGoodsReportResponse>
            >
    getReports() {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .getReports()
        );
    }


    // =====================================================
    // REPORT DETAIL
    // =====================================================

    @GetMapping("/{reportId}")
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_VIEW')"
//    )
    public ResponseEntity<
            DamagedGoodsReportResponse
            >
    getReport(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .getReport(
                                reportId
                        )
        );
    }


    // =====================================================
    // START INSPECTION
    // =====================================================

    @PatchMapping(
            "/{reportId}/start-inspection"
    )
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_HANDLE')"
//    )
    public ResponseEntity<
            DamagedGoodsReportResponse
            >
    startInspection(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .startInspection(
                                reportId
                        )
        );
    }


    // =====================================================
    // HANDLE ONE DAMAGED ITEM
    // =====================================================

    @PatchMapping(
            "/{reportId}/items/{itemId}/handle"
    )
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_HANDLE')"
//    )
    public ResponseEntity<
            DamagedGoodsReportResponse
            >
    handleItem(
            @PathVariable Long reportId,
            @PathVariable Long itemId,

            @Valid
            @RequestBody
            HandleDamagedGoodsRequest request
    ) {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .handleItem(
                                reportId,
                                itemId,
                                request
                        )
        );
    }
}