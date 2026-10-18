
package com.toystorage.backend.controllers.businessstaff;

import com.toystorage.backend.dto.request.businessstaff.SupplierFeedbackRequest;
import com.toystorage.backend.dto.response.businessstaff.SupplierFeedbackResponse;
import com.toystorage.backend.services.businessstaff.BusinessSupplierFeedbackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-staff/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAnyAuthority('ROLE_ROLE-BIZ-STAFF', " +
    "'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-ADMIN')"
)
public class BusinessSupplierFeedbackController {

    private final BusinessSupplierFeedbackService feedbackService;

    @PutMapping("/{purchaseOrderId}/supplier-feedback")
    public ResponseEntity<SupplierFeedbackResponse> recordFeedback(
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody SupplierFeedbackRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                feedbackService.recordFeedback(
                        purchaseOrderId,
                        request,
                        authentication
                )
        );
    }
}
