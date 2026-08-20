package com.toystorage.backend.controllers.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionBatchRequest;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingDetailResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingListResponse;

import com.toystorage.backend.services.receipts.receiving.ReceivingInspectionService;
import com.toystorage.backend.services.receipts.receiving.ReceivingQueryService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/goods-receipts")
@RequiredArgsConstructor
public class ReceivingInspectionController {

    private final ReceivingQueryService
            receivingQueryService;

    private final ReceivingInspectionService
            receivingInspectionService;


    @GetMapping("/receiving")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIVING_VIEW')"
//    )
    public ResponseEntity<List<ReceivingListResponse>>
    getReceivingList() {

        return ResponseEntity.ok(
                receivingQueryService
                        .getReceivingList()
        );
    }


    @GetMapping("/{receiptId}/receiving-detail")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIVING_VIEW')"
//    )
    public ResponseEntity<ReceivingDetailResponse>
    getReceivingDetail(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingQueryService
                        .getReceivingDetail(
                                receiptId
                        )
        );
    }


    @PatchMapping("/{receiptId}/start-receiving")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIVING_HANDLE')"
//    )
    public ResponseEntity<ReceivingDetailResponse>
    startReceiving(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingInspectionService
                        .startReceiving(
                                receiptId
                        )
        );
    }


    @PutMapping("/{receiptId}/inspections")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIVING_HANDLE')"
//    )
    public ResponseEntity<ReceivingDetailResponse>
    saveBatchInspection(
            @PathVariable Long receiptId,

            @Valid
            @RequestBody
            ReceiptInspectionBatchRequest request
    ) {

        return ResponseEntity.ok(
                receivingInspectionService
                        .saveBatchInspection(
                                receiptId,
                                request
                        )
        );
    }


    @PatchMapping("/{receiptId}/finish-inspection")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIVING_HANDLE')"
//    )
    public ResponseEntity<ReceivingDetailResponse>
    finishInspection(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingInspectionService
                        .finishInspection(
                                receiptId
                        )
        );
    }
}