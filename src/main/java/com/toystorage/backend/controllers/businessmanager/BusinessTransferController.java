
package com.toystorage.backend.controllers.businessmanager;

import com.toystorage.backend.dto.response.businessmanager.BusinessTransferDetailResponse;
import com.toystorage.backend.dto.response.businessmanager.BusinessTransferListResponse;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.services.businessmanager.BusinessTransferService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-manager/stock-transfers")
@RequiredArgsConstructor
public class BusinessTransferController {

    private final BusinessTransferService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ROLE-BIZ-MANAGER')")
    public ResponseEntity<Page<BusinessTransferListResponse>> getTransfers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                service.getTransfers(keyword, status, page, size)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ROLE-BIZ-MANAGER')")
    public ResponseEntity<BusinessTransferDetailResponse> getDetail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.getTransferDetail(id)
        );
    }
}
