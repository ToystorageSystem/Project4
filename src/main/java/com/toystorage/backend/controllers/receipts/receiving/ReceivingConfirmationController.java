package com.toystorage.backend.controllers.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.RequestReinspectionRequest;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingConfirmationResponse;
import com.toystorage.backend.services.receipts.receiving.ReceivingConfirmationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class ReceivingConfirmationController {

    private final ReceivingConfirmationService receivingConfirmationService;


    /*
     * =====================================================
     * VIEW INSPECTION RESULT
     * =====================================================
     *
     * Warehouse Manager xem kết quả Staff đã kiểm.
     *
     * INSPECTED:
     * - Có thể review
     * - Có thể request re-inspection
     * - Có thể confirm nếu discrepancy đã được xử lý
     *
     * COMPLETED:
     * - Chỉ xem lịch sử
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_REVIEW')")
    @GetMapping("/{receiptId}/inspection-result")
    public ResponseEntity<ReceivingConfirmationResponse> getInspectionResult(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingConfirmationService
                        .getInspectionResult(receiptId)
        );
    }


    /*
     * =====================================================
     * CONFIRM RECEIVING
     * =====================================================
     *
     * Warehouse Manager only.
     *
     * INSPECTED -> COMPLETED
     *
     * Chỉ được confirm khi:
     * - Staff đã finish inspection
     * - Không còn discrepancy chưa xử lý
     *
     * Inventory chỉ được update tại đây.
     * Putaway chỉ được tạo tại đây.
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_CONFIRM')")
    @PatchMapping("/{receiptId}/confirm-receiving")
    public ResponseEntity<ReceivingConfirmationResponse> confirmReceiving(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingConfirmationService
                        .confirmInspectionResult(receiptId)
        );
    }


    /*
     * =====================================================
     * REQUEST RE-INSPECTION
     * =====================================================
     *
     * Warehouse Manager yêu cầu Warehouse Staff kiểm lại.
     *
     * INSPECTED -> RECEIVING
     *
     * Không update Inventory.
     * Không tạo Putaway.
     *
     * Lần Staff Start Receiving tiếp theo sẽ tạo
     * WarehouseTaskClaim attempt mới.
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_REVIEW')")
    @PatchMapping("/{receiptId}/request-reinspection")
    public ResponseEntity<ReceivingConfirmationResponse> requestReinspection(
            @PathVariable Long receiptId,

            @Valid
            @RequestBody RequestReinspectionRequest request
    ) {

        return ResponseEntity.ok(
                receivingConfirmationService
                        .requestReinspection(
                                receiptId,
                                request
                        )
        );
    }
}