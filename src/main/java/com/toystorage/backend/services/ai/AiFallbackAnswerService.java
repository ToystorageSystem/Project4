
package com.toystorage.backend.services.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.enums.ai.AiIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiFallbackAnswerService {

    private final ObjectMapper objectMapper;

    // =====================================================
    // GENERATE ANSWER
    // =====================================================

    public String generateAnswer(
            AiIntent intent,
            Map<String, Object> analyticsContext
    ) {
        if (intent == null || analyticsContext == null
                || analyticsContext.isEmpty()) {
            return "There is not enough data available for analysis.";
        }

        return switch (intent) {
            case SALES -> buildSalesAnswer(analyticsContext);
            case PRODUCT -> buildProductAnswer(analyticsContext);
            case SUPPLIER -> buildSupplierAnswer(analyticsContext);
            case PURCHASE_ORDER -> buildPurchaseOrderAnswer(analyticsContext);
            case INVENTORY -> buildInventoryAnswer(analyticsContext);
            case STOCK_TRANSFER -> buildStockTransferAnswer(analyticsContext);
            case FORECAST -> buildForecastAnswer(analyticsContext);
            case GENERAL -> buildGeneralAnswer(analyticsContext);

            default -> "The system identified the "
                    + intent.name()
                    + " analytics category and successfully retrieved "
                    + "the related analytics data.";
        };
    }

    // =====================================================
    // GENERAL - Chao hoi va huong dan khi chua ro Intent
    // =====================================================

    private String buildGeneralAnswer(Map<String, Object> context) {
        String question = getString(context, "question");

        if (question != null) {
            String normalized = question.trim()
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[.!?,]+$", "")
                    .trim();

            // Chao hoi don gian, khong goi OpenAI API.
            if ("hello".equals(normalized)
                    || "hi".equals(normalized)
                    || "hey".equals(normalized)
                    || "xin chao".equals(normalized)
                    || "xin chào".equals(normalized)
                    || "chào".equals(normalized)) {
                return "Hello! I can help you analyze inventory, sales, "
                        + "products, purchase orders, suppliers, stock transfers, "
                        + "and product demand forecasts. "
                        + "What would you like to analyze?";
            }
        }

        // Huong dan khi khong nhan dien duoc cau hoi.
        return "I could not identify a specific analytics topic in your question. "
                + "I can analyze inventory, sales, product performance, "
                + "purchase orders, supplier performance, stock transfers, "
                + "and demand forecasts. Please specify what you want to analyze.";
    }

    // =====================================================
    // SALES - Phan tich ban hang
    // =====================================================

    private String buildSalesAnswer(Map<String, Object> context) {
        Map<String, Object> summary =
                convertToMap(context.get("salesSummary"));

        if (summary.isEmpty()) {
            return "No sales data is currently available for analysis.";
        }

        Object storeId = summary.get("storeId");
        long totalOrders = getLong(summary, "totalOrders");
        long draftOrders = getLong(summary, "draftOrders");
        long pendingPaymentOrders = getLong(summary, "pendingPaymentOrders");
        long completedOrders = getLong(summary, "completedOrders");
        long cancelledOrders = getLong(summary, "cancelledOrders");
        long refundedOrders = getLong(summary, "refundedOrders");
        long soldQuantity = getLong(summary, "completedSoldQuantity");

        BigDecimal revenue = getDecimal(summary, "completedRevenue");
        BigDecimal averageOrderValue =
                getDecimal(summary, "averageCompletedOrderValue");

        StringBuilder answer = new StringBuilder();

        if (storeId != null) {
            answer.append("Store ");
            answer.append(storeId);
        } else {
            answer.append("The analyzed stores");
        }

        answer.append(" currently have ");
        answer.append(totalOrders);
        answer.append(totalOrders == 1
                ? " order, including "
                : " orders, including ");
        answer.append(completedOrders);
        answer.append(completedOrders == 1
                ? " completed order"
                : " completed orders");

        if (draftOrders > 0L) {
            answer.append(" and ");
            answer.append(draftOrders);
            answer.append(draftOrders == 1
                    ? " draft order"
                    : " draft orders");
        }

        if (pendingPaymentOrders > 0L) {
            answer.append(", ");
            answer.append(pendingPaymentOrders);
            answer.append(pendingPaymentOrders == 1
                    ? " order awaiting payment"
                    : " orders awaiting payment");
        }

        if (cancelledOrders > 0L) {
            answer.append(", ");
            answer.append(cancelledOrders);
            answer.append(cancelledOrders == 1
                    ? " cancelled order"
                    : " cancelled orders");
        }

        if (refundedOrders > 0L) {
            answer.append(", ");
            answer.append(refundedOrders);
            answer.append(refundedOrders == 1
                    ? " refunded order"
                    : " refunded orders");
        }

        answer.append(". Revenue from completed orders is ");
        answer.append(formatMoney(revenue));
        answer.append(" VND, with ");
        answer.append(soldQuantity);
        answer.append(soldQuantity == 1
                ? " unit sold. "
                : " units sold. ");
        answer.append("The average completed order value is ");
        answer.append(formatMoney(averageOrderValue));
        answer.append(" VND.");

        return answer.toString();
    }

    // =====================================================
    // PRODUCT - Phan tich san pham
    // =====================================================

    private String buildProductAnswer(Map<String, Object> context) {
        List<Map<String, Object>> productRows =
                convertToRows(context.get("productPerformance"));

        if (productRows.isEmpty()) {
            return "No product performance data is currently available for analysis.";
        }

        Object requestedProductId = context.get("requestedProductId");
        Map<String, Object> product =
                findProductPerformance(productRows, requestedProductId);

        if (requestedProductId != null && product == null) {
            return "No product performance data was found for product "
                    + requestedProductId + ".";
        }

        if (product == null) {
            product = findHighestRevenueProduct(productRows);
        }

        if (product == null) {
            return "No product performance data is currently available for analysis.";
        }

        String productName = getString(product, "productName");
        String productCode = getString(product, "productCode");
        String baseUnit = getString(product, "baseUnit");
        String categoryName = getString(product, "categoryName");
        String performanceType = getString(product, "performanceType");

        long soldQuantity = getLong(product, "soldQuantity");
        long completedOrderCount = getLong(product, "completedOrderCount");

        BigDecimal revenue = getDecimal(product, "revenue");
        BigDecimal sellingPrice = getDecimal(product, "sellingPrice");

        String unit = baseUnit == null || baseUnit.isBlank()
                ? "units"
                : baseUnit;

        StringBuilder answer = new StringBuilder();
        answer.append("Product ");
        answer.append(valueOrUnknown(productName));

        if (productCode != null && !productCode.isBlank()) {
            answer.append(" (");
            answer.append(productCode);
            answer.append(")");
        }

        answer.append(" sold ");
        answer.append(soldQuantity);
        answer.append(" ");
        answer.append(unit);
        answer.append(" across ");
        answer.append(completedOrderCount);
        answer.append(completedOrderCount == 1
                ? " completed order"
                : " completed orders");
        answer.append(" and generated ");
        answer.append(formatMoney(revenue));
        answer.append(" VND in revenue.");

        if (sellingPrice.compareTo(BigDecimal.ZERO) > 0) {
            answer.append(" The selling price is ");
            answer.append(formatMoney(sellingPrice));
            answer.append(" VND per ");
            answer.append(unit);
            answer.append(".");
        }

        if (performanceType != null && !performanceType.isBlank()) {
            answer.append(" The product is currently classified as ");
            answer.append(formatPerformanceType(performanceType));
            answer.append(".");
        }

        if (categoryName != null && !categoryName.isBlank()) {
            answer.append(" It belongs to the ");
            answer.append(categoryName);
            answer.append(" category.");
        }

        return answer.toString();
    }

    // =====================================================
    // SUPPLIER - Phan tich nha cung cap
    // =====================================================

    private String buildSupplierAnswer(Map<String, Object> context) {
        List<Map<String, Object>> suppliers =
                convertToRows(context.get("supplierPerformance"));

        if (suppliers.isEmpty()) {
            return "No supplier performance data is currently available for analysis.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append(suppliers.size());
        answer.append(suppliers.size() == 1
                ? " supplier was analyzed. "
                : " suppliers were analyzed. ");

        for (int index = 0; index < suppliers.size(); index++) {
            Map<String, Object> supplier = suppliers.get(index);

            String supplierName = getString(supplier, "supplierName");
            boolean historyAvailable =
                    getBoolean(supplier, "performanceHistoryAvailable");

            long totalDeliveries = getLong(supplier, "totalDeliveries");
            BigDecimal onTimeDeliveryRate =
                    getDecimal(supplier, "onTimeDeliveryRate");
            BigDecimal accuracyRate = getDecimal(supplier, "accuracyRate");

            long totalPurchaseOrders =
                    getLong(supplier, "totalPurchaseOrders");
            long overdueOrders = getLong(supplier, "overdueOrders");
            long pendingResponses = getLong(supplier, "pendingResponses");
            long totalOrderedQuantity =
                    getLong(supplier, "totalOrderedQuantity");
            long totalReceivedQuantity =
                    getLong(supplier, "totalReceivedQuantity");
            BigDecimal receivingRate =
                    getDecimal(supplier, "receivingRate");

            answer.append(valueOrUnknown(supplierName));
            answer.append(" ");

            if (historyAvailable) {
                answer.append("has historical performance data from ");
                answer.append(totalDeliveries);
                answer.append(totalDeliveries == 1
                        ? " delivery"
                        : " deliveries");
                answer.append(", with an on-time delivery rate of ");
                answer.append(formatPercentage(onTimeDeliveryRate));
                answer.append("% and an accuracy rate of ");
                answer.append(formatPercentage(accuracyRate));
                answer.append("%. ");
            } else {
                answer.append("does not yet have historical performance data. ");
            }

            answer.append("The supplier currently has ");
            answer.append(totalPurchaseOrders);
            answer.append(totalPurchaseOrders == 1
                    ? " purchase order"
                    : " purchase orders");

            if (overdueOrders > 0L) {
                answer.append(", including ");
                answer.append(overdueOrders);
                answer.append(overdueOrders == 1
                        ? " overdue order"
                        : " overdue orders");
            }

            if (pendingResponses > 0L) {
                answer.append(", with ");
                answer.append(pendingResponses);
                answer.append(pendingResponses == 1
                        ? " pending supplier response"
                        : " pending supplier responses");
            }

            answer.append(". Received quantity is ");
            answer.append(totalReceivedQuantity);
            answer.append(" of ");
            answer.append(totalOrderedQuantity);
            answer.append(" ordered units, resulting in a receiving rate of ");
            answer.append(formatPercentage(receivingRate));
            answer.append("%.");

            if (index < suppliers.size() - 1) {
                answer.append(" ");
            }
        }

        return answer.toString();
    }

    // =====================================================
    // PURCHASE ORDER - Phan tich don mua hang
    // =====================================================

    private String buildPurchaseOrderAnswer(Map<String, Object> context) {
        List<Map<String, Object>> orders =
                convertToRows(context.get("purchaseOrders"));

        if (orders.isEmpty()) {
            orders = convertToRows(
                    context.get("purchaseOrderAnalytics"));
        }

        if (orders.isEmpty()) {
            return "No purchase order data is currently available for analysis.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append(orders.size());
        answer.append(orders.size() == 1
                ? " purchase order was analyzed. "
                : " purchase orders were analyzed. ");

        for (int index = 0; index < orders.size(); index++) {
            Map<String, Object> order = orders.get(index);

            String orderCode = getString(order, "orderCode");
            String status = getString(order, "status");
            String supplierName = getString(order, "supplierName");
            String warehouseName = getString(order, "warehouseName");
            String supplierResponseStatus =
                    getString(order, "supplierResponseStatus");
            String deliveryStatus = getString(order, "deliveryStatus");

            long overdueDays = getLong(order, "overdueDays");
            long totalOrderedQuantity =
                    getLong(order, "totalOrderedQuantity");
            long totalReceivedQuantity =
                    getLong(order, "totalReceivedQuantity");

            BigDecimal receivingProgressPercent =
                    getDecimal(order, "receivingProgressPercent");
            BigDecimal totalOrderedAmount =
                    getDecimal(order, "totalOrderedAmount");

            answer.append(valueOrUnknown(orderCode));
            answer.append(" is currently ");
            answer.append(valueOrUnknown(status));

            if (supplierName != null && !supplierName.isBlank()) {
                answer.append(" with supplier ");
                answer.append(supplierName);
            }

            if (warehouseName != null && !warehouseName.isBlank()) {
                answer.append(" for ");
                answer.append(warehouseName);
            }

            answer.append(". ");
            answer.append("Receiving progress is ");
            answer.append(formatPercentage(receivingProgressPercent));
            answer.append("% (");
            answer.append(totalReceivedQuantity);
            answer.append(" of ");
            answer.append(totalOrderedQuantity);
            answer.append(" ordered units received). ");

            if (totalOrderedAmount.compareTo(BigDecimal.ZERO) > 0) {
                answer.append("The ordered amount is ");
                answer.append(formatMoney(totalOrderedAmount));
                answer.append(" VND. ");
            }

            if (deliveryStatus != null && !deliveryStatus.isBlank()) {
                answer.append("Delivery status is ");
                answer.append(deliveryStatus);
                answer.append(". ");
            }

            if (overdueDays > 0L) {
                answer.append("The order is ");
                answer.append(overdueDays);
                answer.append(overdueDays == 1
                        ? " day overdue. "
                        : " days overdue. ");
            }

            if (supplierResponseStatus != null
                    && !supplierResponseStatus.isBlank()) {
                answer.append("Supplier response status is ");
                answer.append(supplierResponseStatus);
                answer.append(".");
            }

            if (index < orders.size() - 1) {
                answer.append(" ");
            }
        }

        return answer.toString();
    }

    // =====================================================
    // INVENTORY - Phan tich ton kho
    // =====================================================

    private String buildInventoryAnswer(Map<String, Object> context) {
        List<Map<String, Object>> movementRows =
                convertToRows(context.get("inventoryMovement"));

        Object requestedProductId = context.get("requestedProductId");
        Map<String, Object> productRow =
                findInventoryProduct(movementRows, requestedProductId);

        if (productRow != null) {
            String productName = getString(productRow, "productName");
            String warehouseName = getString(productRow, "warehouseName");
            String baseUnit = getString(productRow, "baseUnit");

            long currentQuantity = getLong(productRow, "currentQuantity");
            long reservedQuantity = getLong(productRow, "reservedQuantity");
            long availableQuantity = getLong(productRow, "availableQuantity");
            long totalInbound = getLong(productRow, "totalInbound");
            long totalOutbound = getLong(productRow, "totalOutbound");
            long netMovement = getLong(productRow, "netMovement");

            String movementStatus =
                    getString(productRow, "movementStatus");

            String unit = baseUnit == null || baseUnit.isBlank()
                    ? "units"
                    : baseUnit;

            StringBuilder answer = new StringBuilder();

            answer.append("Product ");
            answer.append(valueOrUnknown(productName));
            answer.append(" at ");
            answer.append(valueOrUnknown(warehouseName));
            answer.append(" currently has ");
            answer.append(currentQuantity);
            answer.append(" ");
            answer.append(unit);
            answer.append(" in stock, including ");
            answer.append(reservedQuantity);
            answer.append(" ");
            answer.append(unit);
            answer.append(" reserved and ");
            answer.append(availableQuantity);
            answer.append(" ");
            answer.append(unit);
            answer.append(" available.");

            if (totalInbound != 0L
                    || totalOutbound != 0L
                    || netMovement != 0L) {
                answer.append(" Within the analyzed period, total inbound quantity is ");
                answer.append(totalInbound);
                answer.append(" ");
                answer.append(unit);
                answer.append(", total outbound quantity is ");
                answer.append(totalOutbound);
                answer.append(" ");
                answer.append(unit);
                answer.append(", and net inventory movement is ");
                answer.append(netMovement);
                answer.append(" ");
                answer.append(unit);
                answer.append(".");
            }

            if (movementStatus != null && !movementStatus.isBlank()) {
                answer.append(" The current inventory movement status is ");
                answer.append(movementStatus);
                answer.append(".");
            }

            List<Map<String, Object>> anomalies =
                    convertToRows(context.get("inventoryAnomalies"));

            if (!anomalies.isEmpty()) {
                answer.append(" The system detected ");
                answer.append(anomalies.size());
                answer.append(anomalies.size() == 1
                        ? " inventory anomaly that requires attention."
                        : " inventory anomalies that require attention.");
            }

            List<Map<String, Object>> transferRecommendations =
                    convertToRows(
                            context.get("transferRecommendations"));

            if (!transferRecommendations.isEmpty()) {
                answer.append(" The system has ");
                answer.append(transferRecommendations.size());
                answer.append(transferRecommendations.size() == 1
                        ? " stock transfer recommendation."
                        : " stock transfer recommendations.");
            }

            return answer.toString();
        }

        if (requestedProductId != null) {
            return "No inventory data was found for product "
                    + requestedProductId
                    + " at the selected warehouse.";
        }

        Map<String, Object> summary =
                convertToMap(context.get("inventorySummary"));

        if (!summary.isEmpty()) {
            Object warehouseId = summary.get("warehouseId");

            long totalQuantity = getLong(summary, "totalQuantity");
            long totalReservedQuantity =
                    getLong(summary, "totalReservedQuantity");
            long totalAvailableQuantity =
                    getLong(summary, "totalAvailableQuantity");

            return "Warehouse "
                    + valueOrUnknown(warehouseId)
                    + " currently has "
                    + totalQuantity
                    + " units in stock, including "
                    + totalReservedQuantity
                    + " reserved units and "
                    + totalAvailableQuantity
                    + " available units.";
        }

        return "No inventory data is currently available for analysis.";
    }

    // =====================================================
    // STOCK TRANSFER - Phan tich dieu chuyen hang
    // =====================================================

    private String buildStockTransferAnswer(
            Map<String, Object> context
    ) {
        List<Map<String, Object>> transfers =
                convertToRows(context.get("stockTransfers"));

        // Tuong thich voi cac ten du lieu cu.
        if (transfers.isEmpty()) {
            transfers =
                    convertToRows(context.get("stockTransferOperations"));
        }
        if (transfers.isEmpty()) {
            transfers =
                    convertToRows(context.get("transferOperations"));
        }
        if (transfers.isEmpty()) {
            transfers =
                    convertToRows(context.get("transfers"));
        }

        if (transfers.isEmpty()) {
            return "No stock transfer data is currently available for analysis.";
        }

        Map<String, Integer> countsByStatus = new LinkedHashMap<>();
        List<String> overdueReceiptCodes = new ArrayList<>();

        int activeReceiptOverdue = 0;
        int activeShipmentOverdue = 0;
        int shortageTransfers = 0;
        int surplusTransfers = 0;

        long shortageUnits = 0L;
        long surplusUnits = 0L;
        long shippedNotYetReceivedUnits = 0L;

        for (Map<String, Object> transfer : transfers) {
            String rawStatus = getString(transfer, "status");

            String status = (rawStatus == null || rawStatus.isBlank())
                    ? "UNKNOWN"
                    : rawStatus.trim().toUpperCase(Locale.ROOT);

            countsByStatus.merge(status, 1, Integer::sum);

            // Phieu da huy hoac hoan tat khong tinh la dang tre.
            boolean closed =
                    "COMPLETED".equals(status)
                    || "CANCELLED".equals(status);

            if (!closed && getBoolean(transfer, "receiptOverdue")) {
                activeReceiptOverdue++;

                if (overdueReceiptCodes.size() < 3) {
                    String code =
                            getString(transfer, "transferCode");

                    if (code != null && !code.isBlank()) {
                        overdueReceiptCodes.add(code);
                    }
                }
            }

            String shippedAt = getString(transfer, "shippedAt");
            boolean notShipped =
                    shippedAt == null || shippedAt.isBlank();

            if (!closed && notShipped
                    && getBoolean(transfer, "shipmentOverdue")) {
                activeShipmentOverdue++;
            }

            // Hang da xuat nhung van chua nhan du.
            if ("SHIPPED".equals(status)
                    || "RECEIVING".equals(status)) {

                long shipped =
                        getLong(transfer, "totalShippedQuantity");
                long received =
                        getLong(transfer, "totalReceivedQuantity");

                shippedNotYetReceivedUnits +=
                        Math.max(0L, shipped - received);
            }

            long shortage = Math.max(
                    0L,
                    getLong(transfer, "totalShortageQuantity")
            );

            if (shortage > 0L) {
                shortageTransfers++;
                shortageUnits += shortage;
            }

            long surplus = Math.max(
                    0L,
                    getLong(transfer, "totalSurplusQuantity")
            );

            if (surplus > 0L) {
                surplusTransfers++;
                surplusUnits += surplus;
            }
        }

        List<String> statusParts = new ArrayList<>();

        for (Map.Entry<String, Integer> entry
                : countsByStatus.entrySet()) {

            int count = entry.getValue();

            String statusLabel = entry.getKey()
                    .toLowerCase(Locale.ROOT)
                    .replace('_', ' ');

            statusParts.add(count + " " + statusLabel);
        }

        StringBuilder answer = new StringBuilder();

        answer.append(transfers.size());
        answer.append(transfers.size() == 1
                ? " stock transfer was analyzed: "
                : " stock transfers were analyzed: ");
        answer.append(String.join(", ", statusParts));
        answer.append(".");

        if (activeReceiptOverdue > 0) {
            answer.append(" ")
                    .append(activeReceiptOverdue)
                    .append(activeReceiptOverdue == 1
                            ? " open transfer is past its expected receipt date"
                            : " open transfers are past their expected receipt dates");

            if (!overdueReceiptCodes.isEmpty()) {
                answer.append(" (")
                        .append(String.join(", ", overdueReceiptCodes))
                        .append(")");
            }

            answer.append(".");
        }

        if (activeShipmentOverdue > 0) {
            answer.append(" ")
                    .append(activeShipmentOverdue)
                    .append(activeShipmentOverdue == 1
                            ? " open transfer has not shipped by its expected shipment date."
                            : " open transfers have not shipped by their expected shipment dates.");
        }

        if (shippedNotYetReceivedUnits > 0L) {
            answer.append(" ")
                    .append(shippedNotYetReceivedUnits)
                    .append(" shipped units are still awaiting receipt across transfers in SHIPPED or RECEIVING status.");
        }

        if (shortageTransfers > 0) {
            answer.append(" ")
                    .append(shortageTransfers)
                    .append(shortageTransfers == 1
                            ? " transfer recorded a shortage of "
                            : " transfers recorded a total shortage of ")
                    .append(shortageUnits)
                    .append(" units.");
        }

        if (surplusTransfers > 0) {
            answer.append(" ")
                    .append(surplusTransfers)
                    .append(surplusTransfers == 1
                            ? " transfer recorded a surplus of "
                            : " transfers recorded a total surplus of ")
                    .append(surplusUnits)
                    .append(" units.");
        }

        if (activeReceiptOverdue > 0
                || activeShipmentOverdue > 0) {
            answer.append(
                    " Prioritize checking overdue transfers with the source and receiving warehouses."
            );
        }

        if (shortageTransfers > 0 || surplusTransfers > 0) {
            answer.append(
                    " Reconcile shipment and receipt quantities for transfers with discrepancies."
            );
        }

        return answer.toString();
    }

    // =====================================================
    // FORECAST - Phan tich du bao nhu cau san pham
    // =====================================================

    private String buildForecastAnswer(Map<String, Object> context) {
        List<Map<String, Object>> forecasts =
                convertToRows(context.get("forecast"));

        if (forecasts.isEmpty()) {
            return "No product demand forecast data is currently available.";
        }

        StringBuilder answer = new StringBuilder();

        answer.append(forecasts.size())
                .append(forecasts.size() == 1
                        ? " product was analyzed. "
                        : " products were analyzed. ");

        for (Map<String, Object> forecast : forecasts) {
            String productName = getString(forecast, "productName");
            String storeName = getString(forecast, "storeName");
            String baseUnit = getString(forecast, "baseUnit");

            String unit = (baseUnit == null || baseUnit.isBlank())
                    ? "units" : baseUnit;

            long current = getLong(forecast, "currentQuantity");
            long reserved = getLong(forecast, "reservedQuantity");
            long available = getLong(forecast, "availableQuantity");
            long sold = getLong(forecast, "totalSoldQuantity");
            long salesDays = getLong(forecast, "observedSalesDays");
            long forecastDays = getLong(forecast, "forecastPeriodDays");

            answer.append("Product ")
                    .append(valueOrUnknown(productName))
                    .append(" at ")
                    .append(valueOrUnknown(storeName))
                    .append(" currently has ")
                    .append(current)
                    .append(" ")
                    .append(unit)
                    .append(" in stock, including ")
                    .append(reserved)
                    .append(" reserved and ")
                    .append(available)
                    .append(" available. ");

            answer.append("Historical sales: ")
                    .append(sold)
                    .append(" ")
                    .append(unit)
                    .append(" sold across ")
                    .append(salesDays)
                    .append(salesDays == 1
                            ? " observed sales day. "
                            : " observed sales days. ");

            boolean sufficient =
                    getBoolean(forecast, "sufficientHistory");

            Object rawForecast = forecast.get("forecastQuantity");

            // Khong su dung storedResultData
            // neu lich su ban hang chua du.
            if (!sufficient) {
                answer.append(
                        "The system cannot generate a reliable "
                        + "demand forecast because sales history "
                        + "is insufficient. "
                );

                answer.append(
                        "More sales data across multiple days "
                        + "is required before forecasting."
                );

            } else if (!(rawForecast instanceof Number)) {
                answer.append(
                        "A demand forecast is currently unavailable."
                );

                String status = getString(forecast, "forecastStatus");

                if (status != null && !status.isBlank()) {
                    answer.append(" Forecast status: ")
                            .append(status)
                            .append(".");
                }

            } else {
                BigDecimal quantity =
                        getDecimal(forecast, "forecastQuantity");

                answer.append("Forecast demand for the next ")
                        .append(forecastDays)
                        .append(
                                forecastDays == 1
                                        ? " day is "
                                        : " days is "
                        )
                        .append(
                                quantity.stripTrailingZeros().toPlainString()
                        )
                        .append(" ")
                        .append(unit)
                        .append(".");

                Object projected =
                        forecast.get("projectedAvailableAfterForecast");

                if (projected instanceof Number) {
                    answer.append(
                            " Projected available quantity after "
                            + "forecast demand is "
                    );

                    answer.append(
                            getDecimal(
                                    forecast,
                                    "projectedAvailableAfterForecast"
                            ).stripTrailingZeros().toPlainString()
                    );

                    answer.append(" ")
                            .append(unit)
                            .append(".");
                }

                String risk = getString(forecast, "stockRiskStatus");

                if (risk != null && !risk.isBlank()) {
                    answer.append(" Stock risk status: ")
                            .append(risk)
                            .append(".");
                }
            }

            answer.append(" ");
        }

        return answer.toString().trim();
    }

    // =====================================================
    // HELPER - Tim du lieu san pham
    // =====================================================

    private Map<String, Object> findProductPerformance(
            List<Map<String, Object>> rows,
            Object requestedProductId
    ) {
        if (rows == null || rows.isEmpty()
                || requestedProductId == null) {
            return null;
        }

        long targetProductId = toLong(requestedProductId);

        for (Map<String, Object> row : rows) {
            if (getLong(row, "productId") == targetProductId) {
                return row;
            }
        }

        return null;
    }

    private Map<String, Object> findHighestRevenueProduct(
            List<Map<String, Object>> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        Map<String, Object> best = null;
        BigDecimal highestRevenue = null;

        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }

            BigDecimal revenue = getDecimal(row, "revenue");

            if (best == null || highestRevenue == null
                    || revenue.compareTo(highestRevenue) > 0) {

                best = row;
                highestRevenue = revenue;
            }
        }

        return best;
    }

    private String formatPerformanceType(String performanceType) {
        if (performanceType == null || performanceType.isBlank()) {
            return "unknown";
        }

        return switch (performanceType
                .trim()
                .toUpperCase(Locale.ROOT)) {

            case "BEST_SELLING" -> "best-selling";
            case "SLOW_SELLING" -> "slow-selling";
            case "NO_SALES" -> "no-sales";
            case "NORMAL" -> "normal-performing";

            default -> performanceType
                    .trim()
                    .toLowerCase(Locale.ROOT)
                    .replace('_', '-');
        };
    }

    private Map<String, Object> findInventoryProduct(
            List<Map<String, Object>> rows,
            Object requestedProductId
    ) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        if (requestedProductId == null) {
            return rows.get(0);
        }

        long targetProductId = toLong(requestedProductId);

        for (Map<String, Object> row : rows) {
            if (getLong(row, "productId") == targetProductId) {
                return row;
            }
        }

        return null;
    }

    // =====================================================
    // HELPER - Chuyen du lieu thanh danh sach
    // =====================================================

    private List<Map<String, Object>> convertToRows(Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof Collection<?> collection) {
            List<Map<String, Object>> rows = new ArrayList<>();

            for (Object item : collection) {
                if (item == null) {
                    continue;
                }

                Map<String, Object> row = convertToMap(item);

                if (!row.isEmpty()) {
                    rows.add(row);
                }
            }

            return rows;
        }

        Map<String, Object> map = convertToMap(value);

        if (map.isEmpty()) {
            return List.of();
        }

        return List.of(map);
    }

    private Map<String, Object> convertToMap(Object value) {
        if (value == null) {
            return Map.of();
        }

        if (value instanceof Map<?, ?> existingMap) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : existingMap.entrySet()) {
                result.put(
                        String.valueOf(entry.getKey()),
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
    // HELPER - Xu ly so
    // =====================================================

    private long getLong(Map<?, ?> map, String key) {
        if (map == null || key == null) {
            return 0L;
        }

        return toLong(map.get(key));
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        return 0L;
    }

    private BigDecimal getDecimal(Map<?, ?> map, String key) {
        if (map == null || key == null) {
            return BigDecimal.ZERO;
        }

        Object value = map.get(key);

        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private String getString(Map<?, ?> map, String key) {
        if (map == null || key == null) {
            return null;
        }

        Object value = map.get(key);

        return value == null ? null : value.toString();
    }

    private boolean getBoolean(Map<?, ?> map, String key) {
        if (map == null || key == null) {
            return false;
        }

        Object value = map.get(key);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return value != null
                && Boolean.parseBoolean(value.toString());
    }

    // =====================================================
    // HELPER - Dinh dang tien va phan tram
    // =====================================================

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }

        NumberFormat formatter =
                NumberFormat.getNumberInstance(Locale.US);

        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);

        return formatter.format(value);
    }

    private String formatPercentage(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }

        return value
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String valueOrUnknown(Object value) {
        return value == null ? "unknown" : value.toString();
    }
}
