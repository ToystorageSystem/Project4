package com.toystorage.backend.controllers.inventories;


import com.toystorage.backend.dto.request.inventories.EscalateDiscrepancyRequest;
import com.toystorage.backend.dto.request.inventories.ResolveDiscrepancyRequest;
import com.toystorage.backend.dto.response.inventories.DiscrepancyReportResponse;
import com.toystorage.backend.services.inventories.ReceivingDiscrepancyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receiving-discrepancies")
@RequiredArgsConstructor
public class ReceivingDiscrepancyController {

    private final ReceivingDiscrepancyService
            receivingDiscrepancyService;


    /*
     * DANH SÁCH
     */

    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_DISCREPANCY_VIEW')"
//    )
    public ResponseEntity<List<DiscrepancyReportResponse>>
    getReceivingDiscrepancies() {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .getReceivingDiscrepancies()
        );
    }


    /*
     * CHI TIẾT
     */

    @GetMapping("/{discrepancyId}")
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_DISCREPANCY_VIEW')"
//    )
    public ResponseEntity<DiscrepancyReportResponse>
    getDiscrepancy(
            @PathVariable Long discrepancyId
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .getDiscrepancy(discrepancyId)
        );
    }


    /*
     * BẮT ĐẦU XỬ LÝ
     */

    @PatchMapping("/{discrepancyId}/start")
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_DISCREPANCY_HANDLE')"
//    )
    public ResponseEntity<DiscrepancyReportResponse>
    startHandling(
            @PathVariable Long discrepancyId
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .startHandling(discrepancyId)
        );
    }


    /*
     * CHẤP NHẬN THỰC TẾ
     */

    @PatchMapping("/{discrepancyId}/accept-actual")
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_DISCREPANCY_HANDLE')"
//    )
    public ResponseEntity<DiscrepancyReportResponse>
    acceptActualQuantity(
            @PathVariable Long discrepancyId,

            @Valid
            @RequestBody
            ResolveDiscrepancyRequest request
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .acceptActualQuantity(
                                discrepancyId,
                                request
                        )
        );
    }


    /*
     * YÊU CẦU KIỂM LẠI
     */

    @PatchMapping("/{discrepancyId}/recount")
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_DISCREPANCY_HANDLE')"
//    )
    public ResponseEntity<DiscrepancyReportResponse>
    requestRecount(
            @PathVariable Long discrepancyId,

            @Valid
            @RequestBody
            ResolveDiscrepancyRequest request
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .requestRecount(
                                discrepancyId,
                                request
                        )
        );
    }


    /*
     * CHUYỂN BUSINESS MANAGER
     */

    @PatchMapping("/{discrepancyId}/escalate")
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_DISCREPANCY_ESCALATE')"
//    )
    public ResponseEntity<DiscrepancyReportResponse>
    escalate(
            @PathVariable Long discrepancyId,

            @Valid
            @RequestBody
            EscalateDiscrepancyRequest request
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .escalateToBusinessManager(
                                discrepancyId,
                                request
                        )
        );
    }
}