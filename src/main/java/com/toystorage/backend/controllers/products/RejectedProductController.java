package com.toystorage.backend.controllers.products;

import com.toystorage.backend.dto.request.products.ResubmitRejectedProductRequest;
import com.toystorage.backend.dto.response.products.ProductApprovalPageResponse;
import com.toystorage.backend.dto.response.products.ProductApprovalRequestResponse;
import com.toystorage.backend.services.products.RejectedProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/rejected")
@RequiredArgsConstructor
 // @PreAuthorize(
         // "hasAnyAuthority(" +
                 // "'ROLE_ROLE-BIZ-OPS'," +
                 // "'ROLE_ROLE-ADMIN'" +
                 // ")"
 // )
public class RejectedProductController {

    private final RejectedProductService
            rejectedProductService;


    // =====================================================
    // GET REJECTED PRODUCTS
    // =====================================================

    @GetMapping
    public ResponseEntity<ProductApprovalPageResponse>
    getRejectedProducts(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "10"
            )
            Integer size,

            @RequestParam(defaultValue = "false")
            boolean closed
    ) {

        return ResponseEntity.ok(
                rejectedProductService
                        .getRejectedProducts(
                                page,
                                size,
                                closed
                        )
        );
    }


    // =====================================================
    // GET REJECTED PRODUCT DETAIL
    // =====================================================

    @GetMapping("/{requestId}")
    public ResponseEntity<ProductApprovalRequestResponse>
    getRejectedProductDetail(

            @PathVariable
            Long requestId
    ) {

        return ResponseEntity.ok(
                rejectedProductService
                        .getRejectedProductDetail(
                                requestId
                        )
        );
    }


    // =====================================================
    // REJECTED REQUEST - CLOSE FOLLOW-UP
    // =====================================================

    @PostMapping("/{requestId}/close")
    public ResponseEntity<ProductApprovalRequestResponse> closeRejectedRequest(
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(
                rejectedProductService.closeRejectedRequest(requestId)
        );
    }

    // =====================================================
    // RESUBMIT REJECTED PRODUCT
    // =====================================================

    @PostMapping("/{requestId}/resubmit")
    public ResponseEntity<Void> resubmitRejectedProduct(@PathVariable Long requestId) {
        // Quy trình cũ đã ngừng nhận yêu cầu mới; lịch sử vẫn đọc được.
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
