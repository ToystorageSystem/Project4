package com.toystorage.backend.controllers.deliveries.tracking;

import com.toystorage.backend.dto.request.deliveries.tracking
        .UpdateDeliveryLocationRequest;

import com.toystorage.backend.dto.response.deliveries.tracking
        .DeliveryLocationResponse;

import com.toystorage.backend.dto.response.deliveries.tracking
        .DeliveryTrackingResponse;

import com.toystorage.backend.services.deliveries.tracking
        .DeliveryLocationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost
        .PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/deliveries/{deliveryId}/tracking"
)
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final DeliveryLocationService
            deliveryLocationService;


    // =====================================================
    // DELIVERY STAFF UPDATE CURRENT LOCATION
    // =====================================================

    /*
     * Delivery Staff gửi GPS hiện tại.
     *
     * Backend sẽ:
     * - kiểm tra chuyến thuộc đúng driver
     * - kiểm tra delivery đang IN_TRANSIT
     * - cập nhật realtime location
     * - broadcast WebSocket
     * - định kỳ lưu history xuống DB
     */
    @PostMapping("/location")
//    @PreAuthorize(
//            "hasRole('DELIVERY_STAFF')"
//    )
    public ResponseEntity<DeliveryLocationResponse>
    updateLocation(

            @PathVariable
            Long deliveryId,

            @Valid
            @RequestBody
            UpdateDeliveryLocationRequest request
    ) {

        DeliveryLocationResponse response =
                deliveryLocationService
                        .updateLocation(
                                deliveryId,
                                request
                        );


        return ResponseEntity.ok(
                response
        );
    }


    // =====================================================
    // GET LATEST LOCATION
    // =====================================================

    /*
     * API này dùng cho:
     *
     * - Delivery Staff
     * - Warehouse
     * - Store
     * - Business
     *
     * Không kiểm quyền theo role trực tiếp ở Controller.
     *
     * DeliveryTrackingValidationService sẽ kiểm tra:
     * - driver có phải người đang chạy chuyến không
     * - warehouse/store có liên quan đến chuyến không
     * - business có quyền theo dõi không
     */
    @GetMapping("/latest")
//    @PreAuthorize(
//            "isAuthenticated()"
//    )
    public ResponseEntity<DeliveryTrackingResponse>
    getLatestLocation(
            @PathVariable
            Long deliveryId
    ) {

        DeliveryTrackingResponse response =
                deliveryLocationService
                        .getLatestLocation(
                                deliveryId
                        );


        return ResponseEntity.ok(
                response
        );
    }
}