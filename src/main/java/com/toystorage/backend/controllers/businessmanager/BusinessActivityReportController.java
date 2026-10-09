
package com.toystorage.backend.controllers.businessmanager;

import com.toystorage.backend.dto.response.businessmanager.ImportActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.ExportActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.TransferActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.InventoryActivityReportResponse;

import com.toystorage.backend.services.businessmanager.BusinessActivityReportExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.toystorage.backend.services.businessmanager.BusinessActivityReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/business-manager/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ROLE-BIZ-MANAGER')")
public class BusinessActivityReportController {

    private final BusinessActivityReportService reportService;
    private final BusinessActivityReportExcelService excelService;

    // =========================================
    // 1. BAO CAO NHAP HANG
    // =========================================

    @GetMapping("/imports")
    public ResponseEntity<ImportActivityReportResponse> getImportReport(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            Long warehouseId

    ) {

        ImportActivityReportResponse response =
                reportService.getImportReport(
                        fromDate,
                        toDate,
                        warehouseId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================
    // 2. BAO CAO XUAT HANG
    // =========================================

    @GetMapping("/exports")
    public ResponseEntity<ExportActivityReportResponse> getExportReport(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            Long warehouseId

    ) {

        ExportActivityReportResponse response =
                reportService.getExportReport(
                        fromDate,
                        toDate,
                        warehouseId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================
    // 3. BAO CAO DIEU CHUYEN
    // =========================================

    @GetMapping("/transfers")
    public ResponseEntity<TransferActivityReportResponse> getTransferReport(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            Long warehouseId

    ) {

        TransferActivityReportResponse response =
                reportService.getTransferReport(
                        fromDate,
                        toDate,
                        warehouseId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================
    // 4. BAO CAO TON KHO HIEN TAI
    // =========================================

    @GetMapping("/inventory")
    public ResponseEntity<InventoryActivityReportResponse> getInventoryReport(
            @RequestParam(required = false) Long warehouseId
    ) {

        InventoryActivityReportResponse response =
                reportService.getInventoryReport(warehouseId);

        return ResponseEntity.ok(response);
    }

    // =========================================
    // 5. XUAT EXCEL BAO CAO NHAP HANG
    // =========================================

    @GetMapping("/imports/excel")
    public ResponseEntity<byte[]> exportImportExcel(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            Long warehouseId
    ) {

        byte[] data = excelService.exportImports(
                fromDate, toDate, warehouseId
        );

        return excelResponse(
                data, "bao-cao-nhap-hang.xlsx"
        );
    }

    // =========================================
    // 6. XUAT EXCEL BAO CAO XUAT HANG
    // =========================================

    @GetMapping("/exports/excel")
    public ResponseEntity<byte[]> exportExportExcel(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            Long warehouseId
    ) {

        byte[] data = excelService.exportExports(
                fromDate, toDate, warehouseId
        );

        return excelResponse(
                data, "bao-cao-xuat-hang.xlsx"
        );
    }

    // =========================================
    // 7. XUAT EXCEL BAO CAO DIEU CHUYEN
    // =========================================

    @GetMapping("/transfers/excel")
    public ResponseEntity<byte[]> exportTransferExcel(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            Long warehouseId
    ) {

        byte[] data = excelService.exportTransfers(
                fromDate, toDate, warehouseId
        );

        return excelResponse(
                data, "bao-cao-dieu-chuyen.xlsx"
        );
    }

    // =========================================
    // 8. XUAT EXCEL BAO CAO TON KHO
    // =========================================

    @GetMapping("/inventory/excel")
    public ResponseEntity<byte[]> exportInventoryExcel(

            @RequestParam(required = false)
            Long warehouseId
    ) {

        byte[] data = excelService.exportInventory(
                warehouseId
        );

        return excelResponse(
                data, "bao-cao-ton-kho.xlsx"
        );
    }

    // =========================================
    // 9. TRA FILE EXCEL VE CLIENT
    // =========================================

    private ResponseEntity<byte[]> excelResponse(
            byte[] data,
            String filename
    ) {

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentLength(data.length)
                .body(data);
    }


}
