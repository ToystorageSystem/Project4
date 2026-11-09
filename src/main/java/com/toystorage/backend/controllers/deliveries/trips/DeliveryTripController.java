package com.toystorage.backend.controllers.deliveries.trips;

import com.toystorage.backend.dto.request.deliveries.trips
        .RejectDeliveryTripRequest;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripActionResponse;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripDetailResponse;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripListResponse;

import com.toystorage.backend.services.deliveries.trips
        .DeliveryTripService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost
        .PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/delivery-staff/trips"
)
@RequiredArgsConstructor
public class DeliveryTripController {

    private final DeliveryTripService
            deliveryTripService;


    // =====================================================
    // LIST MY TRIPS
    // =====================================================

    @GetMapping
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<
            List<DeliveryTripListResponse>
            >
    getMyDeliveries() {

        return ResponseEntity.ok(
                deliveryTripService
                        .getMyDeliveries()
        );
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @GetMapping(
            "/{deliveryId}"
    )
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryTripDetailResponse>
    getDetail(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                deliveryTripService
                        .getDetail(
                                deliveryId
                        )
        );
    }


    // =====================================================
    // ACCEPT TRIP
    // =====================================================

    @PatchMapping(
            "/{deliveryId}/accept"
    )
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryTripDetailResponse>
    accept(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                deliveryTripService
                        .accept(
                                deliveryId
                        )
        );
    }


    // =====================================================
    // REJECT TRIP
    // =====================================================

    @PatchMapping(
            "/{deliveryId}/reject"
    )
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryTripActionResponse>
    reject(
            @PathVariable
            Long deliveryId,

            @Valid
            @RequestBody
            RejectDeliveryTripRequest request
    ) {

        return ResponseEntity.ok(
                deliveryTripService
                        .reject(
                                deliveryId,
                                request
                        )
        );
    }
    // =====================================================
// ARRIVED
// =====================================================

    @PatchMapping(
            "/{deliveryId}/arrived"
    )
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryTripDetailResponse>
    markArrived(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                deliveryTripService
                        .markArrived(
                                deliveryId
                        )
        );
    }
// =====================================================
// DELIVERED
// =====================================================

    @PatchMapping(
            "/{deliveryId}/delivered"
    )
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryTripDetailResponse>
    completeDelivery(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                deliveryTripService
                        .completeDelivery(
                                deliveryId
                        )
        );
    }

    // =====================================================
// FAILED
// =====================================================

    @PatchMapping(
            "/{deliveryId}/failed"
    )
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryTripDetailResponse>
    failDelivery(
            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                deliveryTripService
                        .failDelivery(
                                deliveryId
                        )
        );
    }
}