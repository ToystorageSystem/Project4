package com.toystorage.backend.controllers.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProgressResponse;
import com.toystorage.backend.services.receipts.receiving.ReceivingMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class ReceivingMonitorController {

    private final ReceivingMonitorService receivingMonitorService;


    // DANH SÁCH PHIẾU ĐANG KIỂM NHẬN

    @GetMapping("/monitoring")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIPT_MONITOR')"
//    )
    public ResponseEntity<List<ReceivingMonitorResponse>>
    getReceivingReceipts() {

        return ResponseEntity.ok(
                receivingMonitorService
                        .getReceivingReceipts()
        );
    }


    // CHI TIẾT TIẾN ĐỘ

    @GetMapping("/{receiptId}/progress")
//    @PreAuthorize(
//            "hasAuthority('GOODS_RECEIPT_MONITOR')"
//    )
    public ResponseEntity<ReceivingProgressResponse>
    getProgress(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingMonitorService
                        .getProgress(receiptId)
        );
    }
}