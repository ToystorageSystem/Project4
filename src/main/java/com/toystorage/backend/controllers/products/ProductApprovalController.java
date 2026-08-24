package com.toystorage.backend.controllers.products;

import com.toystorage.backend.dto.request.products.RejectProductApprovalRequest;
import com.toystorage.backend.dto.response.products.ProductApprovalPageResponse;
import com.toystorage.backend.dto.response.products.ProductApprovalRequestResponse;
import com.toystorage.backend.services.products.ProductApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-approvals")
@RequiredArgsConstructor
public class ProductApprovalController {

    private final ProductApprovalService
            productApprovalService;


    // =====================================================
    // BUSINESS MANAGER - PENDING REQUESTS
    // =====================================================

    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'PRODUCT_APPROVE'," +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<ProductApprovalPageResponse>
    getPendingRequests(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "10"
            )
            Integer size
    ) {

        return ResponseEntity.ok(
                productApprovalService
                        .getPendingRequests(
                                page,
                                size
                        )
        );
    }


    // =====================================================
    // BUSINESS STAFF - MY REQUESTS
    // =====================================================

    @GetMapping("/my-requests")
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'ROLE_BUSINESS_STAFF'," +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<ProductApprovalPageResponse>
    getMyRequests(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "10"
            )
            Integer size
    ) {

        return ResponseEntity.ok(
                productApprovalService
                        .getMyRequests(
                                page,
                                size
                        )
        );
    }


    // =====================================================
    // BUSINESS MANAGER - REQUEST DETAIL
    // =====================================================

    @GetMapping("/{requestId}")
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'PRODUCT_APPROVE'," +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<ProductApprovalRequestResponse>
    getRequestDetail(

            @PathVariable
            Long requestId
    ) {

        return ResponseEntity.ok(
                productApprovalService
                        .getRequestDetail(
                                requestId
                        )
        );
    }


    // =====================================================
    // BUSINESS MANAGER - APPROVE
    // =====================================================

    @PatchMapping("/{requestId}/approve")
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'PRODUCT_APPROVE'," +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<ProductApprovalRequestResponse>
    approve(

            @PathVariable
            Long requestId
    ) {

        return ResponseEntity.ok(
                productApprovalService
                        .approve(
                                requestId
                        )
        );
    }


    // =====================================================
    // BUSINESS MANAGER - REJECT
    // =====================================================

    @PatchMapping("/{requestId}/reject")
    @PreAuthorize(
            "hasAnyAuthority(" +
                    "'PRODUCT_APPROVE'," +
                    "'ROLE_BUSINESS_MANAGER'," +
                    "'ROLE_ADMIN'" +
                    ")"
    )
    public ResponseEntity<ProductApprovalRequestResponse>
    reject(

            @PathVariable
            Long requestId,

            @Valid
            @RequestBody
            RejectProductApprovalRequest request
    ) {

        return ResponseEntity.ok(
                productApprovalService
                        .reject(
                                requestId,
                                request
                        )
        );
    }
}