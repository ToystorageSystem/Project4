package com.toystorage.backend.controllers.inventories.report;

import com.toystorage.backend.dto.response.inventories.report.InventoryProductLocationResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportOptionResponse;
import com.toystorage.backend.dto.response.inventories.report.InventoryReportPageResponse;
import com.toystorage.backend.services.inventories.report.InventoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/inventory-report")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InventoryReportService
            inventoryReportService;


    // =====================================================
    // INVENTORY REPORT LIST
    // =====================================================

    /**
     * Xem báo cáo tồn kho.
     *
     * Hỗ trợ:
     *
     * - tìm theo mã sản phẩm
     * - tìm theo tên sản phẩm
     * - tìm theo barcode
     * - lọc warehouse / store
     * - lọc category
     * - lọc brand
     * - lọc trạng thái tồn kho
     * - pagination
     */
    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority(
                'INVENTORY_REPORT_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<InventoryReportPageResponse>
    getInventoryReport(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long warehouseId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            Long brandId,

            @RequestParam(required = false)
            String stockStatus,

            @RequestParam(required = false)
            Integer page,

            @RequestParam(required = false)
            Integer size
    ) {

        return ResponseEntity.ok(
                inventoryReportService
                        .getInventoryReport(
                                keyword,
                                warehouseId,
                                categoryId,
                                brandId,
                                stockStatus,
                                page,
                                size
                        )
        );
    }


    // =====================================================
    // INVENTORY REPORT - EXPORT EXCEL
    // =====================================================

    /**
     * Xuất báo cáo tồn kho ra file Excel.
     *
     * Export sử dụng cùng search/filter
     * và warehouse permission scope
     * với API danh sách tồn kho.
     */
    @GetMapping(
            "/export"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'INVENTORY_REPORT_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<byte[]>
    exportInventoryReport(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long warehouseId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            Long brandId,

            @RequestParam(required = false)
            String stockStatus
    ) throws IOException {

        byte[] excelFile =
                inventoryReportService
                        .exportInventoryReport(
                                keyword,
                                warehouseId,
                                categoryId,
                                brandId,
                                stockStatus
                        );


        String fileName =
                "inventory-report-"
                        + LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        )
                        + ".xlsx";


        return ResponseEntity
                .ok()

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + fileName
                                + "\""
                )

                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )

                .contentLength(
                        excelFile.length
                )

                .body(
                        excelFile
                );
    }


    // =====================================================
    // PRODUCT INVENTORY BY LOCATIONS
    // =====================================================

    /**
     * Xem tồn của một sản phẩm
     * tại các kho / cửa hàng / vị trí.
     */
    @GetMapping(
            "/products/{productId}/locations"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'INVENTORY_REPORT_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<InventoryProductLocationResponse>>
    getProductLocations(

            @PathVariable
            Long productId
    ) {

        return ResponseEntity.ok(
                inventoryReportService
                        .getProductLocations(
                                productId
                        )
        );
    }


    // =====================================================
    // WAREHOUSE / STORE OPTIONS
    // =====================================================

    /**
     * Danh sách warehouse/store
     * mà user được phép xem.
     */
    @GetMapping(
            "/options/warehouses"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'INVENTORY_REPORT_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<InventoryReportOptionResponse>>
    getWarehouseOptions() {

        return ResponseEntity.ok(
                inventoryReportService
                        .getWarehouseOptions()
        );
    }


    // =====================================================
    // CATEGORY OPTIONS
    // =====================================================

    /**
     * Danh sách category
     * dùng cho filter báo cáo tồn kho.
     */
    @GetMapping(
            "/options/categories"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'INVENTORY_REPORT_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<InventoryReportOptionResponse>>
    getCategoryOptions() {

        return ResponseEntity.ok(
                inventoryReportService
                        .getCategoryOptions()
        );
    }


    // =====================================================
    // BRAND OPTIONS
    // =====================================================

    /**
     * Danh sách brand
     * dùng cho filter báo cáo tồn kho.
     */
    @GetMapping(
            "/options/brands"
    )
    @PreAuthorize("""
            hasAnyAuthority(
                'INVENTORY_REPORT_VIEW',
                'ROLE_BUSINESS_STAFF',
                'ROLE_BUSINESS_MANAGER',
                'ROLE_ADMIN'
            )
            """)
    public ResponseEntity<List<InventoryReportOptionResponse>>
    getBrandOptions() {

        return ResponseEntity.ok(
                inventoryReportService
                        .getBrandOptions()
        );
    }
}