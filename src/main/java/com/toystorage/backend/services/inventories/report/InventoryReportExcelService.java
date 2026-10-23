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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InventoryReportExcelService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private static final String[] HEADERS = {
            "No.",
            "Warehouse Code",
            "Warehouse Name",
            "Warehouse Type",
            "Location Code",
            "Location Name",
            "Product Code",
            "Product Name",
            "Barcode",
            "Category Code",
            "Category Name",
            "Brand Code",
            "Brand Name",
            "On-hand Quantity",
            "Reserved Quantity",
            "Available Quantity",
            "Minimum Stock Level",
            "Stock Status",
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


            createHeader(
                    sheet,
                    headerStyle
            );


            int rowIndex = 1;


            if (items != null) {

                for (
                        InventoryReportItemResponse item
                        : items
                ) {

                    createDataRow(
                            sheet,
                            rowIndex,
                            item
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
            InventoryReportItemResponse item
    ) {

        Row row =
                sheet.createRow(
                        rowIndex
                );


        int columnIndex = 0;


        setIntegerCell(
                row,
                columnIndex++,
                rowIndex
        );


        setStringCell(
                row,
                columnIndex++,
                item.getWarehouseCode()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getWarehouseName()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getWarehouseType()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getLocationCode()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getLocationName()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getProductCode()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getProductName()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getBarcode()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getCategoryCode()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getCategoryName()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getBrandCode()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getBrandName()
        );


        setIntegerCell(
                row,
                columnIndex++,
                item.getOnHandQuantity()
        );


        setIntegerCell(
                row,
                columnIndex++,
                item.getReservedQuantity()
        );


        setIntegerCell(
                row,
                columnIndex++,
                item.getAvailableQuantity()
        );


        setIntegerCell(
                row,
                columnIndex++,
                item.getMinimumStockLevel()
        );


        setStringCell(
                row,
                columnIndex++,
                item.getStockStatus()
        );


        setStringCell(
                row,
                columnIndex,
                item.getLastUpdatedAt() != null
                        ? item.getLastUpdatedAt()
                                .format(
                                        DATE_TIME_FORMATTER
                                )
                        : null
        );
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

    private void configureColumnWidths(
            Sheet sheet
    ) {

        sheet.setColumnWidth(
                0,
                8 * 256
        );

        sheet.setColumnWidth(
                1,
                18 * 256
        );

        sheet.setColumnWidth(
                2,
                25 * 256
        );

        sheet.setColumnWidth(
                3,
                20 * 256
        );

        sheet.setColumnWidth(
                4,
                18 * 256
        );

        sheet.setColumnWidth(
                5,
                25 * 256
        );

        sheet.setColumnWidth(
                6,
                20 * 256
        );

        sheet.setColumnWidth(
                7,
                30 * 256
        );

        sheet.setColumnWidth(
                8,
                20 * 256
        );

        sheet.setColumnWidth(
                9,
                20 * 256
        );

        sheet.setColumnWidth(
                10,
                22 * 256
        );

        sheet.setColumnWidth(
                11,
                20 * 256
        );

        sheet.setColumnWidth(
                12,
                22 * 256
        );

        sheet.setColumnWidth(
                13,
                18 * 256
        );

        sheet.setColumnWidth(
                14,
                18 * 256
        );

        sheet.setColumnWidth(
                15,
                18 * 256
        );

        sheet.setColumnWidth(
                16,
                20 * 256
        );

        sheet.setColumnWidth(
                17,
                18 * 256
        );

        sheet.setColumnWidth(
                18,
                24 * 256
        );
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