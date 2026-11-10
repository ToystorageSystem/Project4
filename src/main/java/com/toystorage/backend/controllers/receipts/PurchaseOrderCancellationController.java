package com.toystorage.backend.controllers.receipts;

import com.toystorage.backend.dto.request.receipts.receiving.RequestPurchaseOrderCancellation;
import com.toystorage.backend.dto.request.receipts.receiving.ReviewPurchaseOrderCancellation;
import com.toystorage.backend.dto.response.receipts.receiving.PurchaseOrderCancellationResponse;
import com.toystorage.backend.enums.receipts.PurchaseOrderCancellationStatus;
import com.toystorage.backend.services.receipts.receiving.PurchaseOrderCancellationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderCancellationController {

    private final PurchaseOrderCancellationService service;

    @PostMapping("/{orderId}/cancellation-requests")
     @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<PurchaseOrderCancellationResponse> create(
            @PathVariable Long orderId,
            @Valid @RequestBody RequestPurchaseOrderCancellation request
    ) {
        // Khong nhan yeu cau cho duyet moi; API da duoc thay bang PATCH /{id}/cancel.
        throw new ResponseStatusException(HttpStatus.GONE,
                "Cancellation approvals are retired. Use PATCH /api/purchase-orders/{orderId}/cancel");
    }

    @GetMapping("/{orderId}/cancellation-requests")
     @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<List<PurchaseOrderCancellationResponse>> listForOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(service.listForOrder(orderId));
    }

    @GetMapping("/cancellation-requests")
     @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<List<PurchaseOrderCancellationResponse>> listForManager(
            @RequestParam(required = false) PurchaseOrderCancellationStatus status
    ) {
        return ResponseEntity.ok(service.listForManager(status));
    }

    @PatchMapping("/cancellation-requests/{requestId}/approve")
     @PreAuthorize("hasAuthority('ROLE_ROLE-ADMIN')")
    public ResponseEntity<PurchaseOrderCancellationResponse> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewPurchaseOrderCancellation request
    ) {
        return ResponseEntity.ok(service.approve(requestId, request));
    }

    @PatchMapping("/cancellation-requests/{requestId}/reject")
     @PreAuthorize("hasAuthority('ROLE_ROLE-ADMIN')")
    public ResponseEntity<PurchaseOrderCancellationResponse> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewPurchaseOrderCancellation request
    ) {
        return ResponseEntity.ok(service.reject(requestId, request));
    }
}
