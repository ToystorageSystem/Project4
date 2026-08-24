package com.toystorage.backend.controllers.shipments;

import com.toystorage.backend.dto.response.shipments.ShipmentManifestDetailResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentManifestPageResponse;
import com.toystorage.backend.services.shipments.ShipmentManifestQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shipment-manifests")
@RequiredArgsConstructor
public class ShipmentManifestController {

    private final ShipmentManifestQueryService
            shipmentManifestQueryService;


    // =====================================================
    // BUSINESS - SHIPMENT MANIFEST LIST
    // =====================================================

    /**
     * Xem danh sách bảng kê đi hàng.
     *
     * Hỗ trợ:
     * - tìm theo mã bảng kê
     * - tìm theo mã phiếu điều chuyển
     * - lọc điểm xuất
     * - lọc điểm nhận
     * - lọc trạng thái vận chuyển
     * - lọc khoảng thời gian
     * - phân trang
     */
    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<ShipmentManifestPageResponse>
    getShipmentManifests(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long fromWarehouseId,

            @RequestParam(required = false)
            Long toWarehouseId,

            @RequestParam(required = false)
            String transportStatus,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate,

            @RequestParam(required = false)
            Integer page,

            @RequestParam(required = false)
            Integer size
    ) {

        return ResponseEntity.ok(
                shipmentManifestQueryService
                        .getShipmentManifests(
                                keyword,
                                fromWarehouseId,
                                toWarehouseId,
                                transportStatus,
                                fromDate,
                                toDate,
                                page,
                                size
                        )
        );
    }


    // =====================================================
    // BUSINESS - SHIPMENT MANIFEST DETAIL
    // =====================================================

    /**
     * Xem chi tiết một bảng kê đi hàng.
     *
     * Bao gồm:
     * - thông tin bảng kê
     * - điểm xuất / điểm nhận
     * - người tạo
     * - trạng thái vận chuyển
     * - driver
     * - danh sách transfer
     * - danh sách package
     * - sản phẩm trong từng package
     */
    @GetMapping("/{manifestId}")
    @PreAuthorize("""
            hasAnyAuthority(
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<ShipmentManifestDetailResponse>
    getShipmentManifestDetail(

            @PathVariable
            Long manifestId
    ) {

        return ResponseEntity.ok(
                shipmentManifestQueryService
                        .getShipmentManifestDetail(
                                manifestId
                        )
        );
    }
}