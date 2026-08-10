package com.toystorage.backend.controllers.packages;

import com.toystorage.backend.dto.response.packages.PackingConfirmationResponse;
import com.toystorage.backend.services.packages.PackingConfirmationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
//    @PreAuthorize(
//            "hasAuthority('PACKING_CONFIRM_VIEW')"
//    )
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
//    @PreAuthorize(
//            "hasAuthority('PACKING_CONFIRM')"
//    )
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
}