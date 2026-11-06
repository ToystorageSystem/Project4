package com.toystorage.backend.controllers.packages.packing.manager;

import com.toystorage.backend.dto.response.packages.packing.PackingConfirmationResponse;
import com.toystorage.backend.services.packages.packing.PackingConfirmationService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
public class PackingConfirmationController {

    private final PackingConfirmationService
            packingConfirmationService;


    /*
     * Xem danh sách kiện +
     * số lượng +
     * seal.
     */
    @GetMapping("/{transferId}/packing-result")
    // Warehouse Manager quan ly kho xuat (service tiep tuc kiem tra kho).
    @PreAuthorize("hasAnyAuthority('ROLE_ROLE-WH-MANAGER', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<PackingConfirmationResponse>
    getPackingResult(
            @PathVariable Long transferId
    ) {

        return ResponseEntity.ok(
                packingConfirmationService
                        .getPackingResult(
                                transferId
                        )
        );
    }


    /*
     * Warehouse Manager confirm.
     */
    @PatchMapping("/{transferId}/confirm-packing")
    @PreAuthorize("hasAnyAuthority('ROLE_ROLE-WH-MANAGER', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<PackingConfirmationResponse>
    confirmPacking(
            @PathVariable Long transferId
    ) {

        return ResponseEntity.ok(
                packingConfirmationService
                        .confirmPacking(
                                transferId
                        )
        );
    }
    @GetMapping("/packing-confirmations")
    @PreAuthorize("hasAnyAuthority('ROLE_ROLE-WH-MANAGER', 'ROLE_ROLE-ADMIN')")
    public ResponseEntity<Page<PackingConfirmationResponse>>
    getPackingConfirmations(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "6")
            int size,

            @RequestParam(defaultValue = "")
            String keyword
    ) {

        return ResponseEntity.ok(
                packingConfirmationService
                        .getPackingConfirmations(
                                page,
                                size,
                                keyword
                        )
        );
    }
}