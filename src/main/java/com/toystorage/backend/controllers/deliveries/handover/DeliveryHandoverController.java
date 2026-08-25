package com.toystorage.backend.controllers.deliveries.handover;

import com.toystorage.backend.dto.request.deliveries.handover.*;
import com.toystorage.backend.dto.response.deliveries.handover
        .DeliveryHandoverResponse;

import com.toystorage.backend.services.deliveries.handover
        .DeliveryHandoverService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/delivery-staff/trips/{deliveryId}/handover"
)
@RequiredArgsConstructor
public class DeliveryHandoverController {

    private final DeliveryHandoverService
            handoverService;


    @PostMapping("/packages/scan")
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryHandoverResponse>
    scanPackage(
            @PathVariable Long deliveryId,

            @Valid
            @RequestBody
            ScanHandoverPackageRequest request
    ) {

        return ResponseEntity.ok(
                handoverService.scanPackage(
                        deliveryId,
                        request
                )
        );
    }


    @PatchMapping("/complete")
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryHandoverResponse>
    complete(
            @PathVariable Long deliveryId,

            @Valid
            @RequestBody
            CompleteDeliveryHandoverRequest request
    ) {

        return ResponseEntity.ok(
                handoverService.complete(
                        deliveryId,
                        request
                )
        );
    }
}