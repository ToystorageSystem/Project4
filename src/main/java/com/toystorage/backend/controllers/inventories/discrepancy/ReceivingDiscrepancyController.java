package com.toystorage.backend.controllers.inventories.discrepancy;


import com.toystorage.backend.dto.request.inventories.discrepancy.EscalateDiscrepancyRequest;
import com.toystorage.backend.dto.request.inventories.discrepancy.ResolveDiscrepancyRequest;
import com.toystorage.backend.dto.response.inventories.discrepancy.DiscrepancyReportResponse;
import com.toystorage.backend.services.inventories.discrepancy.ReceivingDiscrepancyService;
import com.toystorage.backend.dto.request.inventories.discrepancy.UpdateAcceptedQuantityRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
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
    /*
     * CẬP NHẬT SỐ LƯỢNG MANAGER CHẤP NHẬN
     */

    @PatchMapping(
            "/{discrepancyId}/items/{productId}/accepted-quantity"
    )
    public ResponseEntity<DiscrepancyReportResponse>
    updateAcceptedQuantity(
            @PathVariable Long discrepancyId,
            @PathVariable Long productId,

            @Valid
            @RequestBody
            UpdateAcceptedQuantityRequest request
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .updateAcceptedQuantity(
                                discrepancyId,
                                productId,
                                request
                        )
        );
    }
    /*
     * DISCREPANCY CỦA MỘT RECEIPT
     */
    @GetMapping("/receipt/{receiptId}")
    public ResponseEntity<
            List<DiscrepancyReportResponse>
            >
    getByReceipt(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .getByReceipt(
                                receiptId
                        )
        );
    }


    /*
     * CASE ĐÃ CHUYỂN BUSINESS
     */
    @GetMapping("/business")
    public ResponseEntity<
            List<DiscrepancyReportResponse>
            >
    getBusinessQueue() {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .getBusinessQueue()
        );
    }


    /*
     * CASE ĐÃ XỬ LÝ XONG
     */
    @GetMapping("/resolved")
    public ResponseEntity<
            List<DiscrepancyReportResponse>
            >
    getResolvedReports() {

        return ResponseEntity.ok(
                receivingDiscrepancyService
                        .getResolvedReports()
        );
    }
}