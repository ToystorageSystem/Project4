package com.toystorage.backend.controllers.receipts;

import com.toystorage.backend.dto.response.receipts.ReceivingConfirmationResponse;
import com.toystorage.backend.services.receipts.ReceivingConfirmationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class ReceivingConfirmationController {

    private final ReceivingConfirmationService
            receivingConfirmationService;


    /*
     * Xem kết quả inspection
     */

    @GetMapping("/{receiptId}/inspection-result")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIPT_CONFIRM_RECEIVING')"
//    )
    public ResponseEntity<ReceivingConfirmationResponse>
    getInspectionResult(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingConfirmationService
                        .getInspectionResult(receiptId)
        );
    }


    /*
     * Manager xác nhận kết quả.
     *
     * INSPECTED -> COMPLETED
     * Update Inventory
     * Create Putaway
     */

    @PatchMapping("/{receiptId}/confirm-receiving")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIPT_CONFIRM_RECEIVING')"
//    )
    public ResponseEntity<ReceivingConfirmationResponse>
    confirmReceiving(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingConfirmationService
                        .confirmInspectionResult(receiptId)
        );
    }
}