package com.toystorage.backend.controllers.receipts.incidents;

import com.toystorage.backend.dto.request.receipts.incidents.UpdateReceivingIncidentReportItemRequest;
import com.toystorage.backend.dto.request.receipts.incidents.UpdateReceivingIncidentReportRequest;
import com.toystorage.backend.dto.response.receipts.incidents.ReceivingIncidentReportResponse;
import com.toystorage.backend.services.receipts.incidents.ReceivingIncidentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receiving-incident-reports")
@RequiredArgsConstructor
public class ReceivingIncidentReportController {

    private final ReceivingIncidentReportService incidentReportService;

    /*
     * No new @PreAuthorize rules yet.
     * This avoids your current Access Denied issue while testing.
     * Service still requires authenticated user + same warehouse.
     */
    @GetMapping
    public ResponseEntity<Page<ReceivingIncidentReportResponse>> getReports(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                incidentReportService.getMyWarehouseReports(
                        keyword,
                        createPageable(page, size)
                )
        );
    }

    @GetMapping("/goods-receipts/{receiptId}")
    public ResponseEntity<Page<ReceivingIncidentReportResponse>> getByReceipt(
            @PathVariable Long receiptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                incidentReportService.getByReceipt(
                        receiptId,
                        createPageable(page, size)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceivingIncidentReportResponse> getDetail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                incidentReportService.getDetail(id)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReceivingIncidentReportResponse> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReceivingIncidentReportRequest request
    ) {
        return ResponseEntity.ok(
                incidentReportService.updateReport(id, request)
        );
    }

    @PatchMapping("/{reportId}/items/{itemId}")
    public ResponseEntity<ReceivingIncidentReportResponse> updateItem(
            @PathVariable Long reportId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateReceivingIncidentReportItemRequest request
    ) {
        return ResponseEntity.ok(
                incidentReportService.updateItem(
                        reportId,
                        itemId,
                        request
                )
        );
    }

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }
}
