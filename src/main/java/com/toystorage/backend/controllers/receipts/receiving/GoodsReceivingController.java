package com.toystorage.backend.controllers.receipts.receiving;

import com.toystorage.backend.dto.request.receipts.receiving.ReceiptInspectionRequest;
import com.toystorage.backend.dto.response.receipts.receiving.GoodsReceiptResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceiptInspectionResponse;
import com.toystorage.backend.services.receipts.receiving.GoodsReceivingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceivingController {

    private final GoodsReceivingService goodsReceivingService;

    @PatchMapping("/{receiptId}/confirm-arrival")
//    @PreAuthorize("hasAuthority('GOODS_RECEIPT_CONFIRM_ARRIVAL')")
    public ResponseEntity<GoodsReceiptResponse> confirmVehicleArrival(
            @PathVariable Long receiptId
    ) {
        return ResponseEntity.ok(
                goodsReceivingService.confirmVehicleArrival(receiptId)
        );
    }

    @PatchMapping("/{receiptId}/start-receiving")
//    @PreAuthorize("hasAuthority('GOODS_RECEIPT_START_RECEIVING')")
    public ResponseEntity<GoodsReceiptResponse> startReceiving(
            @PathVariable Long receiptId
    ) {
        return ResponseEntity.ok(
                goodsReceivingService.startReceiving(receiptId)
        );
    }

    @PostMapping("/{receiptId}/inspections")
//    @PreAuthorize("hasAuthority('GOODS_RECEIPT_INSPECT')")
    public ResponseEntity<ReceiptInspectionResponse> inspectProduct(
            @PathVariable Long receiptId,
            @Valid @RequestBody ReceiptInspectionRequest request
    ) {
        return ResponseEntity.ok(
                goodsReceivingService.inspectProduct(
                        receiptId,
                        request
                )
        );
    }

    @PatchMapping("/{receiptId}/finish-inspection")
//    @PreAuthorize("hasAuthority('GOODS_RECEIPT_FINISH_INSPECTION')")
    public ResponseEntity<GoodsReceiptResponse> finishInspection(
            @PathVariable Long receiptId
    ) {
        return ResponseEntity.ok(
                goodsReceivingService.finishInspection(receiptId)
        );
    }
}