package com.toystorage.backend.controllers.transfers.picking;

import com.toystorage.backend.dto.request.transfers.picking.PickTransferItemRequest;

import com.toystorage.backend.dto.response.transfers.picking.TransferPickingItemResponse;
import com.toystorage.backend.dto.response.transfers.picking.TransferPickingResponse;

import com.toystorage.backend.services.transfers.picking.TransferPickingService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/warehouse/picking"
)
@RequiredArgsConstructor
public class TransferPickingController {

    private final TransferPickingService
            transferPickingService;


    // =====================================================
    // LIST
    // =====================================================

    @GetMapping("/transfers")
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PICKING_VIEW')"
//    )
    public ResponseEntity<List<TransferPickingResponse>>
    getTransfers() {

        return ResponseEntity.ok(
                transferPickingService
                        .getMyWarehouseTransfers()
        );
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @GetMapping(
            "/transfers/{transferId}"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PICKING_VIEW')"
//    )
    public ResponseEntity<TransferPickingResponse>
    getTransfer(
            @PathVariable
            Long transferId
    ) {

        return ResponseEntity.ok(
                transferPickingService
                        .getTransfer(
                                transferId
                        )
        );
    }


    // =====================================================
    // START
    // =====================================================

    @PatchMapping(
            "/transfers/{transferId}/start"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PICKING_HANDLE')"
//    )
    public ResponseEntity<TransferPickingResponse>
    startPicking(
            @PathVariable
            Long transferId
    ) {

        return ResponseEntity.ok(
                transferPickingService
                        .startPicking(
                                transferId
                        )
        );
    }


    // =====================================================
    // PICK ITEM
    // =====================================================

    @PatchMapping(
            "/transfers/{transferId}/items/{itemId}"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PICKING_HANDLE')"
//    )
    public ResponseEntity<TransferPickingItemResponse>
    pickItem(
            @PathVariable
            Long transferId,

            @PathVariable
            Long itemId,

            @Valid
            @RequestBody
            PickTransferItemRequest request
    ) {

        return ResponseEntity.ok(
                transferPickingService
                        .pickItem(
                                transferId,
                                itemId,
                                request
                        )
        );
    }


    // =====================================================
    // COMPLETE
    // =====================================================

    @PatchMapping(
            "/transfers/{transferId}/complete"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PICKING_HANDLE')"
//    )
    public ResponseEntity<TransferPickingResponse>
    completePicking(
            @PathVariable
            Long transferId
    ) {

        return ResponseEntity.ok(
                transferPickingService
                        .completePicking(
                                transferId
                        )
        );
    }
}