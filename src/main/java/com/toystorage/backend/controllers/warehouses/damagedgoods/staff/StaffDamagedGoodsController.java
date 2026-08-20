package com.toystorage.backend.controllers.warehouses.damagedgoods.staff;

import com.toystorage.backend.dto.request.warehouses.damagedgoods.CreateStaffDamagedGoodsRequest;

import com.toystorage.backend.dto.response.warehouses.damagedgoods.StaffDamagedGoodsReportResponse;

import com.toystorage.backend.services.warehouses.damagedgoods.StaffDamagedGoodsService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(
        "/api/warehouse/staff/damaged-goods"
)
@RequiredArgsConstructor
public class StaffDamagedGoodsController {

    private final StaffDamagedGoodsService
            damagedGoodsService;


    // =====================================================
    // CREATE REPORT
    // =====================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_REPORT')"
//    )
    public ResponseEntity<StaffDamagedGoodsReportResponse>
    create(
            @Valid
            @RequestPart("request")
            CreateStaffDamagedGoodsRequest request,

            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {

        return ResponseEntity.ok(
                damagedGoodsService
                        .create(
                                request,
                                image
                        )
        );
    }


    // =====================================================
    // MY REPORTS
    // =====================================================

    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_VIEW')"
//    )
    public ResponseEntity<
            List<StaffDamagedGoodsReportResponse>
            >
    getMyReports() {

        return ResponseEntity.ok(
                damagedGoodsService
                        .getMyReports()
        );
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @GetMapping(
            "/{reportId}"
    )
//    @PreAuthorize(
//            "hasAuthority('DAMAGED_GOODS_VIEW')"
//    )
    public ResponseEntity<StaffDamagedGoodsReportResponse>
    getDetail(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                damagedGoodsService
                        .getDetail(
                                reportId
                        )
        );
    }
}