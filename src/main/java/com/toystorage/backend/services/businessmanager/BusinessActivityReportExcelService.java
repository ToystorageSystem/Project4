
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

        summary.put("Tong phieu nhap",
                report.getTotalReceipts());

        summary.put("Phieu da hoan thanh",
                report.getCompletedReceipts());

        summary.put("Tong so luong da nhan",
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
            "Ma phieu nhap",
            "ID kho",
            "Ten kho",
            "Trang thai",
            "Ngay tao",
            "Ngay nhan",
            "So luong accepted"
        };

        return createWorkbook(
                "BAO CAO NHAP HANG",
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

        summary.put("Tong phieu xuat",
                report.getTotalExports());

        summary.put("Phieu da shipped",
                report.getShippedExports());

        summary.put("Tong so luong shipped",
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
            "Ma phieu",
            "ID kho gui",
            "Ten kho gui",
            "ID kho nhan",
            "Ten kho nhan",
            "Trang thai",
            "Ngay tao",
            "Ngay shipped",
            "So luong shipped"
        };

        return createWorkbook(
                "BAO CAO XUAT HANG",
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

        summary.put("Tong phieu dieu chuyen",
                report.getTotalTransfers());

        summary.put("Phieu da hoan thanh",
                report.getCompletedTransfers());

        summary.put("Tong so luong shipped",
                report.getTotalShippedQuantity());

        for (Map.Entry<String, Long> entry
                : report.getTransfersByStatus().entrySet()) {

            summary.put(
                    "Trang thai " + entry.getKey(),
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
            "Ma phieu",
            "ID kho gui",
            "Ten kho gui",
            "ID kho nhan",
            "Ten kho nhan",
            "Trang thai",
            "Ngay tao",
            "Ngay shipped",
            "So luong shipped"
        };

        return createWorkbook(
                "BAO CAO DIEU CHUYEN",
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

        summary.put("Thoi diem bao cao",
                report.getSnapshotAt());

        summary.put("ID kho",
                warehouseId == null ? "Tat ca" : warehouseId);

        summary.put("So san pham khac nhau",
                report.getTotalProducts());

        summary.put("So dong ton kho",
                report.getTotalBalanceRecords());

        summary.put("Tong ton vat ly",
                report.getTotalQuantity());

        summary.put("Tong ton giu cho",
                report.getTotalReservedQuantity());

        summary.put("Tong ton kha dung",
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
            "ID ton kho",
            "ID kho",
            "Ten kho",
            "ID vi tri",
            "Ma vi tri",
            "Ten vi tri",
            "Loai vi tri",
            "ID san pham",
            "Ma san pham",
            "Ten san pham",
            "Ton vat ly",
            "Ton giu cho",
            "Ton kha dung"
        };

        return createWorkbook(
                "BAO CAO TON KHO HIEN TAI",
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

        filters.put("Tu ngay",
                fromDate == null ? "Tat ca" : fromDate);

        filters.put("Den ngay",
                toDate == null ? "Tat ca" : toDate);

        filters.put("ID kho",
                warehouseId == null ? "Tat ca" : warehouseId);

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
                    workbook.createSheet("Tong quan");

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
                    workbook.createSheet("Chi tiet");

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
