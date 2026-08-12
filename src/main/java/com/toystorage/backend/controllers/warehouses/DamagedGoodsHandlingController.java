package com.toystorage.backend.controllers.warehouses;
import com.toystorage.backend.dto.request.warehouses.HandleDamagedGoodsRequest;
import com.toystorage.backend.dto.response.warehouses.DamagedGoodsReportResponse;
import com.toystorage.backend.services.warehouses.DamagedGoodsHandlingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/damaged-goods")
@RequiredArgsConstructor
public class DamagedGoodsHandlingController {

    private final DamagedGoodsHandlingService
            damagedGoodsHandlingService;


    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_VIEW')"
//    )
    public ResponseEntity<List<DamagedGoodsReportResponse>>
    getReports() {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .getReports()
        );
    }


    @GetMapping("/{reportId}")
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_VIEW')"
//    )
    public ResponseEntity<DamagedGoodsReportResponse>
    getReport(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .getReport(reportId)
        );
    }


    @PatchMapping("/{reportId}/start-inspection")
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_HANDLE')"
//    )
    public ResponseEntity<DamagedGoodsReportResponse>
    startInspection(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                damagedGoodsHandlingService
                        .startInspection(reportId)
        );
    }


    @PatchMapping("/{reportId}/items/{itemId}/handle")
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_HANDLE')"
//    )
    public ResponseEntity<DamagedGoodsReportResponse>
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