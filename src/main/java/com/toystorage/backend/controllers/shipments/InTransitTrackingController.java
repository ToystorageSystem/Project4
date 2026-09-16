package com.toystorage.backend.controllers.shipments;

import com.toystorage.backend.dto.response.shipments.InTransitShipmentDetailResponse;
import com.toystorage.backend.dto.response.shipments.InTransitShipmentPageResponse;
import com.toystorage.backend.services.shipments.InTransitTrackingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/in-transit-shipments")
@RequiredArgsConstructor
public class InTransitTrackingController {

    private final InTransitTrackingService
            inTransitTrackingService;


    // =====================================================
    // TASK #13 - LIST IN-TRANSIT SHIPMENTS
    // =====================================================

    /**
     * Business xem danh sách các chuyến vận chuyển.
     *
     * Hỗ trợ:
     * - tìm mã chuyến
     * - tìm mã manifest
     * - tìm mã stock transfer
     * - tìm mã package
     * - lọc điểm xuất
     * - lọc điểm nhận
     * - lọc trạng thái
     * - phân trang
     */
    @GetMapping
    @PreAuthorize("""
           hasAnyAuthority(
            'ROLE_BS_STAFF',
            'ROLE_BS_MANAGER',
            'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<InTransitShipmentPageResponse>
    getShipments(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long fromWarehouseId,

            @RequestParam(required = false)
            Long toWarehouseId,

            @RequestParam(required = false)
            String status,

            @RequestParam(required = false)
            Integer page,

            @RequestParam(required = false)
            Integer size
    ) {

        return ResponseEntity.ok(
                inTransitTrackingService
                        .getShipments(
                                keyword,
                                fromWarehouseId,
                                toWarehouseId,
                                status,
                                page,
                                size
                        )
        );
    }


    // =====================================================
    // TASK #13 - SHIPMENT DETAIL
    // =====================================================

    /**
     * Xem chi tiết một chuyến vận chuyển.
     *
     * Bao gồm:
     * - delivery
     * - manifest
     * - điểm xuất / điểm nhận
     * - driver
     * - thời gian vận chuyển
     * - cảnh báo quá hạn
     * - transfer + sản phẩm
     * - package + sản phẩm
     * - lịch sử trạng thái hiện có
     */
    @GetMapping("/{deliveryId}")
    @PreAuthorize("""
           hasAnyAuthority(
                        'ROLE_BS_STAFF',
                        'ROLE_BS_MANAGER',
                        'ROLE_ADMIN'
                        )
            """)
    public ResponseEntity<InTransitShipmentDetailResponse>
    getShipmentDetail(

            @PathVariable
            Long deliveryId
    ) {

        return ResponseEntity.ok(
                inTransitTrackingService
                        .getShipmentDetail(
                                deliveryId
                        )
        );
    }
}