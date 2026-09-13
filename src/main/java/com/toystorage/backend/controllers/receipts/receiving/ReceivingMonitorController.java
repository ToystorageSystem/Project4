package com.toystorage.backend.controllers.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProgressResponse;
import com.toystorage.backend.services.receipts.receiving.ReceivingMonitorService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class ReceivingMonitorController {

    private final ReceivingMonitorService receivingMonitorService;


    /*
     * =====================================================
     * IN PROGRESS
     * RECEIVING
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_MONITOR')")
@GetMapping("/monitoring")
public ResponseEntity<Page<ReceivingMonitorResponse>>
getReceivingReceipts(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
) {

    Pageable pageable = createPageable(page, size);

    return ResponseEntity.ok(
            receivingMonitorService.getReceivingReceipts(
                    keyword,
                    pageable
            )
    );
}


    /*
     * =====================================================
     * WAITING FOR REVIEW
     * INSPECTED
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_REVIEW')")
    @GetMapping("/waiting-review")
    public ResponseEntity<Page<ReceivingMonitorResponse>>
    getWaitingReviewReceipts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "")
            String keyword,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        return ResponseEntity.ok(
                receivingMonitorService
                        .getWaitingReviewReceipts(
                                keyword,
                                pageable
                        )
        );
    }


    /*
     * =====================================================
     * COMPLETED
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_REVIEW')")
    @GetMapping("/completed-receiving")
    public ResponseEntity<Page<ReceivingMonitorResponse>>
    getCompletedReceipts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "")
            String keyword,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        return ResponseEntity.ok(
                receivingMonitorService
                        .getCompletedReceipts(
                                keyword,
                                pageable
                        )
        );
    }


    /*
     * =====================================================
     * RECEIVING PROGRESS DETAIL
     * =====================================================
     */

//    @PreAuthorize("hasAuthority('GOODS_RECEIVING_MONITOR')")
    @GetMapping("/{receiptId}/progress")
    public ResponseEntity<ReceivingProgressResponse>
    getProgress(
            @PathVariable Long receiptId
    ) {

        return ResponseEntity.ok(
                receivingMonitorService
                        .getProgress(
                                receiptId
                        )
        );
    }


    /*
     * =====================================================
     * PAGINATION
     * =====================================================
     */

    private Pageable createPageable(
            int page,
            int size
    ) {

        int safePage =
                Math.max(
                        page,
                        0
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "updatedAt"
                )
        );
    }
}