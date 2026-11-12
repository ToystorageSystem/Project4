package com.toystorage.backend.services.inventories.report;

import com.toystorage.backend.dto.response.inventories.report.InventoryReportItemResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryReportExcelService {

    // Moi dong la tong ton cua mot san pham tai mot kho / cua hang.
    private static final String[] HEADERS = {
            "No.",
            "Warehouse / Store",
            "Warehouse Code",
            "Warehouse Type",
            "Product Code",
            "Product Name",
            "Barcode",
            "Base Unit",
            "Category",
            "Brand",
            "Physical Stock",
            "Reserved Stock",
            "Available Stock",
            "Last Updated At"
    };


    // =====================================================
    // EXPORT INVENTORY REPORT
    // =====================================================

    public byte[] export(
            List<InventoryReportItemResponse> items
    ) throws IOException {

        try (
                Workbook workbook =
                        new XSSFWorkbook();

                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            Sheet sheet =
                    workbook.createSheet(
                            "Inventory Report"
                    );


            // Giữ header khi scroll.
            sheet.createFreezePane(
                    0,
                    1
            );


            CellStyle headerStyle =
                    createHeaderStyle(
                            workbook
                    );

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper()
                    .createDataFormat().getFormat("dd/mm/yyyy hh:mm:ss"));


            createHeader(
                    sheet,
                    headerStyle
            );


            int rowIndex = 1;


            if (items != null) {

                for (InventoryReportItemResponse item : aggregateByWarehouseAndProduct(items)) {

                    createDataRow(
                            sheet,
                            rowIndex,
                            item,
                            dateStyle
                    );

                    rowIndex++;
                }
            }


            if (rowIndex > 1) {

                sheet.setAutoFilter(
                        new CellRangeAddress(
                                0,
                                rowIndex - 1,
                                0,
                                HEADERS.length - 1
                        )
                );
            }


            configureColumnWidths(
                    sheet
            );


            workbook.write(
                    outputStream
            );


            return outputStream.toByteArray();
        }
    }


    // =====================================================
    // TONG HOP THEO (KHO, SAN PHAM) - KHONG TACH THEO KE
    // =====================================================

    private List<InventoryReportItemResponse> aggregateByWarehouseAndProduct(
            List<InventoryReportItemResponse> items
    ) {
        Map<String, InventoryReportItemResponse> grouped = new LinkedHashMap<>();

        for (InventoryReportItemResponse item : items) {
            if (item == null) continue;

            // ID la dinh danh chinh. Fallback ma chi de xu ly du lieu cu bi thieu ID.
            String warehouseKey = item.getWarehouseId() != null
                    ? item.getWarehouseId().toString()
                    : "code:" + item.getWarehouseCode();
            String productKey = item.getProductId() != null
                    ? item.getProductId().toString()
                    : "code:" + item.getProductCode();
            String key = warehouseKey + "|" + productKey;

            InventoryReportItemResponse total = grouped.get(key);
            if (total == null) {
                total = InventoryReportItemResponse.builder()
                        .warehouseId(item.getWarehouseId())
                        .warehouseCode(item.getWarehouseCode())
                        .warehouseName(item.getWarehouseName())
                        .warehouseType(item.getWarehouseType())
                        .productId(item.getProductId())
                        .productCode(item.getProductCode())
                        .productName(item.getProductName())
                        .barcode(item.getBarcode())
                        .baseUnit(item.getBaseUnit())
                        .categoryName(item.getCategoryName())
                        .brandName(item.getBrandName())
                        .onHandQuantity(0)
                        .reservedQuantity(0)
                        .availableQuantity(0)
                        .lastUpdatedAt(item.getLastUpdatedAt())
                        .build();
                grouped.put(key, total);
            }

            // Cong 3 chi so doc lap; KHONG suy ra Available = Physical - Reserved.
            total.setOnHandQuantity(total.getOnHandQuantity() + orZero(item.getOnHandQuantity()));
            total.setReservedQuantity(total.getReservedQuantity() + orZero(item.getReservedQuantity()));
            total.setAvailableQuantity(total.getAvailableQuantity() + orZero(item.getAvailableQuantity()));

            if (item.getLastUpdatedAt() != null
                    && (total.getLastUpdatedAt() == null
                    || item.getLastUpdatedAt().isAfter(total.getLastUpdatedAt()))) {
                total.setLastUpdatedAt(item.getLastUpdatedAt());
            }
        }

        List<InventoryReportItemResponse> result = new ArrayList<>(grouped.values());
        result.sort(Comparator
                .comparing(InventoryReportItemResponse::getWarehouseName,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(InventoryReportItemResponse::getProductName,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }


    // =====================================================
    // HEADER
    // =====================================================

    private void createHeader(
            Sheet sheet,
            CellStyle headerStyle
    ) {

        Row headerRow =
                sheet.createRow(
                        0
                );


        headerRow.setHeightInPoints(
                24
        );


        for (
                int columnIndex = 0;
                columnIndex < HEADERS.length;
                columnIndex++
        ) {

            Cell cell =
                    headerRow.createCell(
                            columnIndex
                    );


            cell.setCellValue(
                    HEADERS[columnIndex]
            );


            cell.setCellStyle(
                    headerStyle
            );
        }
    }


    // =====================================================
    // DATA ROW
    // =====================================================

    private void createDataRow(
            Sheet sheet,
            int rowIndex,
            InventoryReportItemResponse item,
            CellStyle dateStyle
    ) {
        Row row = sheet.createRow(rowIndex);
        int columnIndex = 0;

        setIntegerCell(row, columnIndex++, rowIndex);
        setStringCell(row, columnIndex++, item.getWarehouseName());
        setStringCell(row, columnIndex++, item.getWarehouseCode());
        setStringCell(row, columnIndex++, item.getWarehouseType());
        setStringCell(row, columnIndex++, item.getProductCode());
        setStringCell(row, columnIndex++, item.getProductName());
        setStringCell(row, columnIndex++, item.getBarcode());
        setStringCell(row, columnIndex++, item.getBaseUnit());
        setStringCell(row, columnIndex++, item.getCategoryName());
        setStringCell(row, columnIndex++, item.getBrandName());
        setIntegerCell(row, columnIndex++, item.getOnHandQuantity());
        setIntegerCell(row, columnIndex++, item.getReservedQuantity());
        setIntegerCell(row, columnIndex++, item.getAvailableQuantity());

        if (item.getLastUpdatedAt() != null) {
            Cell dateCell = row.createCell(columnIndex);
            dateCell.setCellValue(item.getLastUpdatedAt());
            dateCell.setCellStyle(dateStyle);
        }
    }


    // =====================================================
    // HEADER STYLE
    // =====================================================

    private CellStyle createHeaderStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();


        Font font =
                workbook.createFont();


        font.setBold(
                true
        );


        style.setFont(
                font
        );


        style.setAlignment(
                HorizontalAlignment.CENTER
        );


        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );


        style.setWrapText(
                true
        );


        return style;
    }


    // =====================================================
    // COLUMN WIDTH
    // =====================================================

    private void configureColumnWidths(Sheet sheet) {
        int[] widths = {8, 28, 20, 19, 23, 33, 19, 14, 26, 23, 18, 18, 18, 24};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }


    // =====================================================
    // CELL HELPERS
    // =====================================================

    private void setStringCell(
            Row row,
            int columnIndex,
            String value
    ) {

        Cell cell =
                row.createCell(
                        columnIndex
                );


        cell.setCellValue(
                value != null
                        ? value
                        : ""
        );
    }


    private void setIntegerCell(
            Row row,
            int columnIndex,
            Integer value
    ) {

        Cell cell =
                row.createCell(
                        columnIndex
                );


        cell.setCellValue(
                value != null
                        ? value
                        : 0
        );
    }
}