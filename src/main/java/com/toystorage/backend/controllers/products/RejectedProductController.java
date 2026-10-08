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
@PreAuthorize(
        "hasAnyAuthority(" +
                "'ROLE_BUSINESS_STAFF'," +
                "'ROLE_ADMIN'" +
                ")"
)
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
            Integer size
    ) {

        return ResponseEntity.ok(
                rejectedProductService
                        .getRejectedProducts(
                                page,
                                size
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
    // RESUBMIT REJECTED PRODUCT
    // =====================================================

    @PostMapping("/{requestId}/resubmit")
    public ResponseEntity<ProductApprovalRequestResponse>
    resubmitRejectedProduct(

            @PathVariable
            Long requestId,

            @Valid
            @RequestBody
            ResubmitRejectedProductRequest request
    ) {

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        rejectedProductService
                                .resubmit(
                                        requestId,
                                        request
                                )
                );
    }
}