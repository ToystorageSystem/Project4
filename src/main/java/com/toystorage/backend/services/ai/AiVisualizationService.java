
package com.toystorage.backend.services.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.enums.ai.AiIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiVisualizationService {

    private final ObjectMapper objectMapper;

    // =====================================================
    // BUILD TABLE DATA
    // Tao du lieu bang cho Frontend
    // =====================================================

    public List<Map<String, Object>> buildTableData(
            AiIntent intent,
            Map<String, Object> context
    ) {

        if (intent == null
                || context == null
                || context.isEmpty()) {

            return List.of();
        }

        return switch (intent) {

            // SALES - Du lieu ban hang
            case SALES -> extractFirstAvailable(
                    context,
                    "dailySalesTrend",
                    "monthlySalesTrend",
                    "salesSummary"
            );

            // PRODUCT - Hieu suat san pham
            case PRODUCT -> buildProductTableData(
                    context
            );

            // INVENTORY - Ton kho
            case INVENTORY -> extractFirstAvailable(
                    context,
                    "inventoryMovement",
                    "slowMovingInventory",
                    "transferRecommendations",
                    "inventoryAnomalies",
                    "inventorySummary"
            );

            // SUPPLIER - Nha cung cap
            case SUPPLIER -> extractFirstAvailable(
                    context,
                    "supplierPerformance",
                    "suppliers"
            );

            // PURCHASE ORDER - Don mua hang
            case PURCHASE_ORDER -> extractFirstAvailable(
                    context,
                    "purchaseOrders",
                    "purchaseOrderAnalytics"
            );

            // STOCK TRANSFER - Dieu chuyen hang
            case STOCK_TRANSFER -> extractFirstAvailable(
                    context,
                    "stockTransfers",
                    "stockTransferOperations",
                    "transferOperations",
                    "transfers"
            );

            // FORECAST - Du bao nhu cau
            case FORECAST -> extractFirstAvailable(
                    context,
                    "forecastAnalytics",
                    "forecast"
            );

            // GENERAL - Khong co bang thong ke
            case GENERAL -> List.of();
        };
    }

    // =====================================================
    // BUILD CHART METADATA
    // Tao cau hinh bieu do cho Frontend
    // =====================================================

    public Map<String, Object> buildChartMetadata(
            AiIntent intent,
            Map<String, Object> context
    ) {

        if (intent == null
                || context == null
                || context.isEmpty()) {

            return Map.of();
        }

        Map<String, Object> chart =
                new LinkedHashMap<>();

        switch (intent) {

            // =================================================
            // SALES
            // =================================================

            case SALES -> {

                chart.put(
                        "type",
                        "LINE"
                );

                chart.put(
                        "title",
                        "Daily Revenue Trend"
                );

                chart.put(
                        "dataSource",
                        "dailySalesTrend"
                );

                chart.put(
                        "xAxis",
                        "period"
                );

                chart.put(
                        "yAxis",
                        "revenue"
                );
            }

            // =================================================
            // PRODUCT
            // =================================================

            case PRODUCT -> {

                chart.put(
                        "type",
                        "BAR"
                );

                chart.put(
                        "title",
                        "Product Sales Performance"
                );

                chart.put(
                        "dataSource",
                        "productPerformance"
                );

                chart.put(
                        "xAxis",
                        "productName"
                );

                chart.put(
                        "yAxis",
                        "revenue"
                );
            }

            // =================================================
            // INVENTORY
            // =================================================

            case INVENTORY -> {

                chart.put(
                        "type",
                        "BAR"
                );

                chart.put(
                        "title",
                        "Inventory Movement"
                );

                chart.put(
                        "dataSource",
                        "inventoryMovement"
                );

                chart.put(
                        "xAxis",
                        "productName"
                );

                chart.put(
                        "yAxis",
                        "netMovement"
                );
            }

            // =================================================
            // SUPPLIER
            // =================================================

            case SUPPLIER -> {

                chart.put(
                        "type",
                        "BAR"
                );

                chart.put(
                        "title",
                        "Supplier Performance"
                );

                chart.put(
                        "dataSource",
                        "supplierPerformance"
                );

                chart.put(
                        "xAxis",
                        "supplierName"
                );

                chart.put(
                        "yAxis",
                        "onTimeDeliveryRate"
                );
            }

            // =================================================
            // PURCHASE ORDER
            // =================================================

            case PURCHASE_ORDER -> {

                chart.put(
                        "type",
                        "BAR"
                );

                chart.put(
                        "title",
                        "Purchase Order Progress"
                );

                chart.put(
                        "dataSource",
                        "purchaseOrders"
                );

                chart.put(
                        "xAxis",
                        "orderCode"
                );

                chart.put(
                        "yAxis",
                        "receivingProgressPercent"
                );
            }

            // =================================================
            // STOCK TRANSFER
            // =================================================

            case STOCK_TRANSFER -> {

                chart.put(
                        "type",
                        "BAR"
                );

                chart.put(
                        "title",
                        "Stock Transfer Progress"
                );

                chart.put(
                        "dataSource",
                        "stockTransfers"
                );

                chart.put(
                        "xAxis",
                        "transferCode"
                );

                chart.put(
                        "yAxis",
                        "receivingProgressPercent"
                );
            }

            // =================================================
            // FORECAST
            // Chi ve bieu do khi co du bao hop le
            // =================================================

            case FORECAST -> {

                List<Map<String, Object>> forecastRows =
                        extractFirstAvailable(
                                context,
                                "forecast"
                        );

                // Kiem tra co it nhat mot dong du bao hop le.
                boolean hasValidForecast =
                        forecastRows.stream()
                                .anyMatch(row ->

                                        Boolean.TRUE.equals(
                                                row.get("sufficientHistory")
                                        )

                                        && row.get("forecastQuantity")
                                                instanceof Number
                                );

                // Neu chua du lich su ban hang,
                // khong ve bieu do du bao.
                if (!hasValidForecast) {

                    return Map.of();
                }

                // Du bao theo san pham,
                // khong su dung field period khong ton tai.
                chart.put(
                        "type",
                        "BAR"
                );

                chart.put(
                        "title",
                        "Product Demand Forecast"
                );

                chart.put(
                        "dataSource",
                        "forecast"
                );

                chart.put(
                        "xAxis",
                        "productName"
                );

                chart.put(
                        "yAxis",
                        "forecastQuantity"
                );
            }

            // =================================================
            // GENERAL
            // =================================================

            case GENERAL -> {

                return Map.of();
            }
        }

        return chart;
    }

    // =====================================================
    // PRODUCT TABLE DATA
    // Neu co productId chi tra dung san pham do
    // =====================================================

    private List<Map<String, Object>> buildProductTableData(
            Map<String, Object> context
    ) {

        if (context == null
                || context.isEmpty()) {

            return List.of();
        }

        List<Map<String, Object>> productRows =
                convertToRows(
                        context.get("productPerformance")
                );

        Object requestedProductId =
                context.get("requestedProductId");

        if (productRows.isEmpty()) {

            if (requestedProductId != null) {

                return List.of();
            }

            return convertToRows(
                    context.get("categorySales")
            );
        }

        // Khong chon san pham cu the,
        // tra ve tat ca san pham.
        if (requestedProductId == null) {

            return productRows;
        }

        long targetProductId =
                toLong(requestedProductId);

        for (Map<String, Object> row : productRows) {

            long productId =
                    getLong(
                            row,
                            "productId"
                    );

            if (productId == targetProductId) {

                return List.of(row);
            }
        }

        // Khong tim thay san pham thi khong tra nham.
        return List.of();
    }

    // =====================================================
    // EXTRACT FIRST AVAILABLE
    // Tim du lieu co san dau tien trong context
    // =====================================================

    private List<Map<String, Object>> extractFirstAvailable(
            Map<String, Object> context,
            String... keys
    ) {

        if (context == null
                || context.isEmpty()
                || keys == null) {

            return List.of();
        }

        for (String key : keys) {

            if (key == null || key.isBlank()) {

                continue;
            }

            Object value =
                    context.get(key);

            if (value == null) {

                continue;
            }

            List<Map<String, Object>> rows =
                    convertToRows(value);

            if (!rows.isEmpty()) {

                return rows;
            }
        }

        return List.of();
    }

    // =====================================================
    // CONVERT VALUE TO ROWS
    // Chuyen DTO hoac List thanh danh sach Map
    // =====================================================

    private List<Map<String, Object>> convertToRows(
            Object value
    ) {

        if (value == null) {

            return List.of();
        }

        // Neu la Collection, chuyen tung phan tu.
        if (value instanceof Collection<?> collection) {

            List<Map<String, Object>> rows =
                    new ArrayList<>();

            for (Object item : collection) {

                if (item == null) {

                    continue;
                }

                Map<String, Object> row =
                        convertToMap(item);

                if (!row.isEmpty()) {

                    rows.add(row);
                }
            }

            return rows;
        }

        // Neu la DTO hoac Map.
        Map<String, Object> map =
                convertToMap(value);

        if (map.isEmpty()) {

            return List.of();
        }

        // Kiem tra cac danh sach long ben trong.
        String[] nestedListKeys = {
                "records",
                "items",
                "data",
                "results",
                "products",
                "categories",
                "suppliers",
                "purchaseOrders",
                "transfers",
                "forecasts"
        };

        for (String nestedKey : nestedListKeys) {

            Object nestedValue =
                    map.get(nestedKey);

            if (nestedValue instanceof Collection<?>) {

                return convertToRows(nestedValue);
            }
        }

        return List.of(map);
    }

    // =====================================================
    // CONVERT OBJECT TO MAP
    // =====================================================

    private Map<String, Object> convertToMap(
            Object value
    ) {

        if (value == null) {

            return Map.of();
        }

        if (value instanceof Map<?, ?> existingMap) {

            Map<String, Object> result =
                    new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry
                    : existingMap.entrySet()) {

                result.put(
                        String.valueOf(
                                entry.getKey()
                        ),
                        entry.getValue()
                );
            }

            return result;
        }

        return objectMapper.convertValue(
                value,
                new TypeReference<Map<String, Object>>() {}
        );
    }

    // =====================================================
    // NUMBER HELPERS
    // =====================================================

    private long getLong(
            Map<?, ?> map,
            String key
    ) {

        if (map == null || key == null) {

            return 0L;
        }

        return toLong(
                map.get(key)
        );
    }

    private long toLong(Object value) {

        if (value instanceof Number number) {

            return number.longValue();
        }

        if (value != null) {

            try {

                return Long.parseLong(
                        value.toString()
                );

            } catch (NumberFormatException ignored) {

                return 0L;
            }
        }

        return 0L;
    }
}
