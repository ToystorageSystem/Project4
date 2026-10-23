
package com.toystorage.backend.controllers.businessmanager;

import com.toystorage.backend.dto.request.businessmanager.ApproveStoreReturnRequest;
import com.toystorage.backend.services.businessmanager.ApproveStoreReturnService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-manager/store-returns")
@RequiredArgsConstructor
public class ApproveStoreReturnController {

    private final ApproveStoreReturnService service;

    // Issue #19: Business Manager approves Store Return
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ROLE-BIZ-MANAGER')")
    public ResponseEntity<Void> approveStoreReturn(
            @PathVariable Long id,
            @Valid @RequestBody ApproveStoreReturnRequest request
    ) {

        service.approve(id, request);

        return ResponseEntity.noContent().build();
    }
}
