package com.toystorage.backend.controllers.shipments;

import com.toystorage.backend.dto.request.shipments.ConfirmShipmentRequest;
import com.toystorage.backend.dto.response.shipments.ShipmentConfirmationResponse;
import com.toystorage.backend.services.shipments.ShipmentConfirmationService;
import com.toystorage.backend.dto.response.packages.shipments.DispatchHandoverListResponse;

import java.util.List;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
public class ShipmentConfirmationController {

    private final ShipmentConfirmationService
            shipmentConfirmationService;


    @GetMapping("/{transferId}/shipment")
//    @PreAuthorize(
//            "hasAuthority('SHIPMENT_CONFIRM_VIEW')"
//    )
    public ResponseEntity<ShipmentConfirmationResponse>
    getShipment(
            @PathVariable Long transferId
    ) {

        return ResponseEntity.ok(
                shipmentConfirmationService
                        .getShipment(transferId)
        );
    }


    @PatchMapping("/{transferId}/confirm-shipment")
//    @PreAuthorize(
//            "hasAuthority('SHIPMENT_CONFIRM')"
//    )
    public ResponseEntity<ShipmentConfirmationResponse>
    confirmShipment(
            @PathVariable Long transferId,

            @Valid
            @RequestBody
            ConfirmShipmentRequest request
    ) {

        return ResponseEntity.ok(
                shipmentConfirmationService
                        .confirmShipment(
                                transferId,
                                request
                        )
        );
    }

    @GetMapping("/dispatch-handover")
    public ResponseEntity<
            List<DispatchHandoverListResponse>
            >
    getDispatchHandoverList(
            @RequestParam(
                    defaultValue = ""
            )
            String keyword
    ) {

        return ResponseEntity.ok(
                shipmentConfirmationService
                        .getDispatchHandoverList(
                                keyword
                        )
        );
    }
}