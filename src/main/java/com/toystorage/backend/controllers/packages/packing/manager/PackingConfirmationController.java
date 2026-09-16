package com.toystorage.backend.controllers.packages.packing.manager;

import com.toystorage.backend.dto.response.packages.packing.PackingConfirmationResponse;
import com.toystorage.backend.services.packages.packing.PackingConfirmationService;
import org.springframework.data.domain.Page;
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
    @GetMapping("/packing-confirmations")
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