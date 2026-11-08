
package com.toystorage.backend.services.businessmanager;

import com.toystorage.backend.dto.response.businessmanager.ImportActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.ExportActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.TransferActivityReportResponse;
import com.toystorage.backend.dto.response.businessmanager.InventoryActivityReportResponse;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessActivityReportExcelService {

    private final BusinessActivityReportService reportService;

    // ==========================================
    // 1. XUAT EXCEL BAO CAO NHAP HANG
    // ==========================================

    public byte[] exportImports(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        ImportActivityReportResponse report =
                reportService.getImportReport(
                        fromDate, toDate, warehouseId
                );

        Map<String, Object> summary =
                createFilters(fromDate, toDate, warehouseId);

        summary.put("Total Goods Receipts",
                report.getTotalReceipts());

        summary.put("Completed Receipts",
                report.getCompletedReceipts());

        summary.put("Total Accepted Quantity",
                report.getTotalAcceptedQuantity());

        List<Object[]> rows = new ArrayList<>();

        for (ImportActivityReportResponse.ImportRecord item
                : report.getRecords()) {

            rows.add(new Object[]{
                item.getId(),
                item.getReceiptCode(),
                item.getWarehouseId(),
                item.getWarehouseName(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getReceivedAt(),
                item.getAcceptedQuantity()
            });
        }

        String[] headers = {
            "ID",
            "Goods Receipt Code",
            "Warehouse ID",
            "Warehouse Name",
            "Status",
            "Created At",
            "Received At",
            "Accepted Quantity"
        };

        return createWorkbook(
                "GOODS RECEIPT REPORT",
                summary, headers, rows
        );
    }

    // ==========================================
    // 2. XUAT EXCEL BAO CAO XUAT HANG
    // ==========================================

    public byte[] exportExports(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        ExportActivityReportResponse report =
                reportService.getExportReport(
                        fromDate, toDate, warehouseId
                );

        Map<String, Object> summary =
                createFilters(fromDate, toDate, warehouseId);

        summary.put("Total Outbound Transfers",
                report.getTotalExports());

        summary.put("Shipped Transfers",
                report.getShippedExports());

        summary.put("Total Shipped Quantity",
                report.getTotalShippedQuantity());

        List<Object[]> rows = new ArrayList<>();

        for (ExportActivityReportResponse.ExportRecord item
                : report.getRecords()) {

            rows.add(new Object[]{
                item.getId(),
                item.getTransferCode(),
                item.getFromWarehouseId(),
                item.getFromWarehouseName(),
                item.getToWarehouseId(),
                item.getToWarehouseName(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getShippedAt(),
                item.getShippedQuantity()
            });
        }

        String[] headers = {
            "ID",
            "Stock Transfer Code",
            "Source Warehouse ID",
            "Source Warehouse",
            "Destination Warehouse ID",
            "Destination Warehouse",
            "Status",
            "Created At",
            "Shipped At",
            "Shipped Quantity"
        };

        return createWorkbook(
                "OUTBOUND STOCK REPORT",
                summary, headers, rows
        );
    }

    // ==========================================
    // 3. XUAT EXCEL BAO CAO DIEU CHUYEN
    // ==========================================

    public byte[] exportTransfers(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        TransferActivityReportResponse report =
                reportService.getTransferReport(
                        fromDate, toDate, warehouseId
                );

        Map<String, Object> summary =
                createFilters(fromDate, toDate, warehouseId);

        summary.put("Total Stock Transfers",
                report.getTotalTransfers());

        summary.put("Completed Transfers",
                report.getCompletedTransfers());

        summary.put("Total Shipped Quantity",
                report.getTotalShippedQuantity());

        for (Map.Entry<String, Long> entry
                : report.getTransfersByStatus().entrySet()) {

            summary.put(
                    "Status " + entry.getKey(),
                    entry.getValue()
            );
        }

        List<Object[]> rows = new ArrayList<>();

        for (TransferActivityReportResponse.TransferRecord item
                : report.getRecords()) {

            rows.add(new Object[]{
                item.getId(),
                item.getTransferCode(),
                item.getFromWarehouseId(),
                item.getFromWarehouseName(),
                item.getToWarehouseId(),
                item.getToWarehouseName(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getShippedAt(),
                item.getShippedQuantity()
            });
        }

        String[] headers = {
            "ID",
            "Stock Transfer Code",
            "Source Warehouse ID",
            "Source Warehouse",
            "Destination Warehouse ID",
            "Destination Warehouse",
            "Status",
            "Created At",
            "Shipped At",
            "Shipped Quantity"
        };

        return createWorkbook(
                "STOCK TRANSFER REPORT",
                summary, headers, rows
        );
    }

    // ==========================================
    // 4. XUAT EXCEL BAO CAO TON KHO
    // ==========================================

    public byte[] exportInventory(Long warehouseId) {

        InventoryActivityReportResponse report =
                reportService.getInventoryReport(warehouseId);

        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("Snapshot At",
                report.getSnapshotAt());

        summary.put("Warehouse ID",
                warehouseId == null ? "All" : warehouseId);

        summary.put("Distinct Products",
                report.getTotalProducts());

        summary.put("Inventory Location Records",
                report.getTotalBalanceRecords());

        summary.put("Total Physical Stock",
                report.getTotalQuantity());

        summary.put("Total Reserved Stock",
                report.getTotalReservedQuantity());

        summary.put("Total Available Stock",
                report.getTotalAvailableQuantity());

        List<Object[]> rows = new ArrayList<>();

        for (InventoryActivityReportResponse.InventoryRecord item
                : report.getRecords()) {

            rows.add(new Object[]{
                item.getInventoryBalanceId(),
                item.getWarehouseId(),
                item.getWarehouseName(),
                item.getLocationId(),
                item.getLocationCode(),
                item.getLocationName(),
                item.getLocationType(),
                item.getProductId(),
                item.getProductCode(),
                item.getProductName(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getAvailableQuantity()
            });
        }

        String[] headers = {
            "Inventory Balance ID",
            "Warehouse ID",
            "Warehouse Name",
            "Location ID",
            "Location Code",
            "Location Name",
            "Location Type",
            "Product ID",
            "Product Code",
            "Product Name",
            "Physical Stock",
            "Reserved Stock",
            "Available Stock"
        };

        return createWorkbook(
                "CURRENT INVENTORY REPORT",
                summary, headers, rows
        );
    }

    // ==========================================
    // 5. TAO THONG TIN BO LOC
    // ==========================================

    private Map<String, Object> createFilters(
            LocalDate fromDate,
            LocalDate toDate,
            Long warehouseId
    ) {

        Map<String, Object> filters = new LinkedHashMap<>();

        filters.put("From Date",
                fromDate == null ? "All" : fromDate);

        filters.put("To Date",
                toDate == null ? "All" : toDate);

        filters.put("Warehouse ID",
                warehouseId == null ? "All" : warehouseId);

        return filters;
    }

    // ==========================================
    // 6. TAO FILE EXCEL
    // ==========================================

    private byte[] createWorkbook(
            String reportName,
            Map<String, Object> summary,
            String[] headers,
            List<Object[]> dataRows
    ) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            // Style cho dong tieu de
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex()
            );
            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            // SHEET 1: TONG QUAN
            Sheet summarySheet =
                    workbook.createSheet("Summary");

            Row titleRow = summarySheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(reportName);
            titleCell.setCellStyle(headerStyle);

            int summaryRowIndex = 2;

            for (Map.Entry<String, Object> entry
                    : summary.entrySet()) {

                Row row =
                        summarySheet.createRow(summaryRowIndex++);

                writeCell(row, 0, entry.getKey());
                writeCell(row, 1, entry.getValue());
            }

            summarySheet.setColumnWidth(0, 36 * 256);
            summarySheet.setColumnWidth(1, 32 * 256);

            // SHEET 2: CHI TIET
            Sheet detailSheet =
                    workbook.createSheet("Details");

            Row headerRow = detailSheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);

                detailSheet.setColumnWidth(i, 28 * 256);
            }

            int rowIndex = 1;

            for (Object[] values : dataRows) {

                Row row = detailSheet.createRow(rowIndex++);

                for (int i = 0;
                        i < values.length && i < headers.length;
                        i++) {

                    writeCell(row, i, values[i]);
                }
            }

            detailSheet.createFreezePane(0, 1);

            if (rowIndex > 1) {
                detailSheet.setAutoFilter(
                        new org.apache.poi.ss.util.CellRangeAddress(
                                0, rowIndex - 1,
                                0, headers.length - 1
                        )
                );
            }

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Cannot create Excel report", e
            );
        }
    }

    // ==========================================
    // 7. GHI GIA TRI VAO CELL
    // ==========================================

    private void writeCell(
            Row row,
            int columnIndex,
            Object value
    ) {

        Cell cell = row.createCell(columnIndex);

        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }
}
