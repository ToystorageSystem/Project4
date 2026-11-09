package com.toystorage.backend.services.ai;

import com.toystorage.backend.dto.request.ai.AiChatRequest;
import com.toystorage.backend.enums.ai.AiIntent;
import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.services.analytics.CategoryAnalyticsService;
import com.toystorage.backend.services.analytics.ForecastAnalyticsService;
import com.toystorage.backend.services.analytics.InventoryAnalyticsService;
import com.toystorage.backend.services.analytics.InventoryAnomalyAnalyticsService;
import com.toystorage.backend.services.analytics.InventoryMovementAnalyticsService;
import com.toystorage.backend.services.analytics.InventoryTransferRecommendationService;
import com.toystorage.backend.services.analytics.ProductAnalyticsService;
import com.toystorage.backend.services.analytics.PurchaseOrderAnalyticsService;
import com.toystorage.backend.services.analytics.SalesAnalyticsService;
import com.toystorage.backend.services.analytics.SlowMovingInventoryAnalyticsService;
import com.toystorage.backend.services.analytics.StockTransferOperationalAnalyticsService;
import com.toystorage.backend.services.analytics.SupplierPerformanceAnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAnalyticsContextService {

    private final InventoryAnalyticsService
            inventoryAnalyticsService;

    private final InventoryMovementAnalyticsService
            inventoryMovementAnalyticsService;

    private final SlowMovingInventoryAnalyticsService
            slowMovingInventoryAnalyticsService;

    private final InventoryTransferRecommendationService
            inventoryTransferRecommendationService;

    private final InventoryAnomalyAnalyticsService
            inventoryAnomalyAnalyticsService;

    private final SalesAnalyticsService
            salesAnalyticsService;

    private final ProductAnalyticsService
            productAnalyticsService;

    private final CategoryAnalyticsService
            categoryAnalyticsService;

    private final PurchaseOrderAnalyticsService
            purchaseOrderAnalyticsService;

    private final SupplierPerformanceAnalyticsService
            supplierPerformanceAnalyticsService;

    private final StockTransferOperationalAnalyticsService
            stockTransferOperationalAnalyticsService;

    private final ForecastAnalyticsService
            forecastAnalyticsService;


    // =========================================================
    // BUILD ANALYTICS CONTEXT
    //
    // Đây là lớp trung gian giữa:
    //
    // AI Intent
    //      ↓
    // Analytics Services
    //      ↓
    // dữ liệu thật
    //
    // LLM KHÔNG truy cập DB trực tiếp.
    // =========================================================

    public Map<String, Object> buildContext(

            AiIntent intent,

            AiChatRequest request
    ) {

        if (
                request == null
        ) {

            throw new BadRequest(
                    "AI request không được để trống"
            );
        }


        AiIntent effectiveIntent =
                intent == null
                        ? AiIntent.GENERAL
                        : intent;


        return switch (
                effectiveIntent
        ) {

            case INVENTORY ->
                    buildInventoryContext(
                            request
                    );

            case SALES ->
                    buildSalesContext(
                            request
                    );

            case PRODUCT ->
                    buildProductContext(
                            request
                    );

            case PURCHASE_ORDER ->
                    buildPurchaseOrderContext(
                            request
                    );

            case SUPPLIER ->
                    buildSupplierContext(
                            request
                    );

            case STOCK_TRANSFER ->
                    buildStockTransferContext(
                            request
                    );

            case FORECAST ->
                    buildForecastContext(
                            request
                    );

            case GENERAL ->
                    buildGeneralContext(
                            request
                    );
        };
    }


    // =========================================================
    // INVENTORY
    // =========================================================

    private Map<String, Object> buildInventoryContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.INVENTORY,
                        request
                );


        Long warehouseId =
                resolveWarehouseId(
                        request
                );


        context.put(
                "resolvedWarehouseId",
                warehouseId
        );


        // =====================================================
        // INVENTORY SUMMARY
        // =====================================================

        context.put(
                "inventorySummary",
                inventoryAnalyticsService
                        .getInventorySummary(
                                warehouseId
                        )
        );


        // =====================================================
        // INVENTORY MOVEMENT
        // =====================================================

        context.put(
                "inventoryMovement",
                inventoryMovementAnalyticsService
                        .getInventoryMovement(
                                warehouseId,
                                request.getProductId(),
                                null,
                                null
                        )
        );


        // =====================================================
        // SLOW-MOVING
        // =====================================================

        context.put(
                "slowMovingInventory",
                slowMovingInventoryAnalyticsService
                        .getSlowMovingInventory(
                                warehouseId,
                                request.getProductId(),
                                null,
                                null
                        )
        );


        // =====================================================
        // ANOMALIES
        // =====================================================

        context.put(
                "inventoryAnomalies",
                inventoryAnomalyAnalyticsService
                        .getInventoryAnomalies(
                                warehouseId,
                                request.getProductId()
                        )
        );


        // =====================================================
        // TRANSFER RECOMMENDATIONS
        //
        // Chỉ recommendation.
        // Không tạo Stock Transfer.
        // =====================================================

        context.put(
                "transferRecommendations",
                inventoryTransferRecommendationService
                        .getTransferRecommendations(
                                request.getProductId(),
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // SALES
    // =========================================================

    private Map<String, Object> buildSalesContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.SALES,
                        request
                );


        Long storeId =
                request.getStoreId();


        // =====================================================
        // SALES SUMMARY
        // =====================================================

        context.put(
                "salesSummary",
                salesAnalyticsService
                        .getSalesSummary(
                                storeId,
                                null,
                                null
                        )
        );


        // =====================================================
        // DAILY TREND
        // =====================================================

        context.put(
                "dailySalesTrend",
                salesAnalyticsService
                        .getDailySalesTrend(
                                storeId,
                                null,
                                null
                        )
        );


        // =====================================================
        // MONTHLY TREND
        // =====================================================

        context.put(
                "monthlySalesTrend",
                salesAnalyticsService
                        .getMonthlySalesTrend(
                                storeId,
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // PRODUCT
    // =========================================================

    private Map<String, Object> buildProductContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.PRODUCT,
                        request
                );


        Long storeId =
                request.getStoreId();


        // =====================================================
        // PRODUCT PERFORMANCE
        //
        // ProductAnalytics hiện filter theo:
        //
        // - store
        // - category
        //
        // productId vẫn được giữ trong request context để
        // AI biết người dùng đang quan tâm sản phẩm nào.
        // =====================================================

        context.put(
                "productPerformance",
                productAnalyticsService
                        .getProductPerformance(
                                storeId,
                                null,
                                null,
                                null
                        )
        );


        // =====================================================
        // CATEGORY SALES
        // =====================================================

        context.put(
                "categorySales",
                categoryAnalyticsService
                        .getCategorySales(
                                storeId,
                                null,
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // PURCHASE ORDER
    // =========================================================

    private Map<String, Object> buildPurchaseOrderContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.PURCHASE_ORDER,
                        request
                );


        Long warehouseId =
                resolveWarehouseId(
                        request
                );


        context.put(
                "resolvedWarehouseId",
                warehouseId
        );


        context.put(
                "purchaseOrders",
                purchaseOrderAnalyticsService
                        .getPurchaseOrderAnalytics(
                                null,
                                warehouseId,
                                null,
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // SUPPLIER
    // =========================================================

    private Map<String, Object> buildSupplierContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.SUPPLIER,
                        request
                );


        // AiChatRequest hiện chưa có supplierId,
        // vì vậy lấy danh sách supplier analytics để AI
        // phân tích dựa trên câu hỏi.
        //
        // Sau này nếu cần ta có thể bổ sung supplierId.

        context.put(
                "supplierPerformance",
                supplierPerformanceAnalyticsService
                        .getSupplierPerformanceAnalytics(
                                null,
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // STOCK TRANSFER
    // =========================================================

    private Map<String, Object> buildStockTransferContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.STOCK_TRANSFER,
                        request
                );


        // Không tự suy đoán:
        //
        // warehouseId = fromWarehouse
        // storeId     = toWarehouse
        //
        // vì câu hỏi người dùng có thể hỏi transfer theo
        // bất kỳ chiều nào.
        //
        // Do đó giai đoạn đầu lấy operational analytics
        // và để AI phân tích trên dữ liệu backend đã kiểm soát.

        context.put(
                "stockTransfers",
                stockTransferOperationalAnalyticsService
                        .getOperationalAnalytics(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // FORECAST
    // =========================================================

    private Map<String, Object> buildForecastContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.FORECAST,
                        request
                );


        context.put(
                "forecast",
                forecastAnalyticsService
                        .getForecastAnalytics(
                                request.getStoreId(),
                                request.getProductId(),
                                null,
                                null,
                                null
                        )
        );


        return context;
    }


    // =========================================================
    // GENERAL
    // =========================================================

    private Map<String, Object> buildGeneralContext(
            AiChatRequest request
    ) {

        Map<String, Object> context =
                createBaseContext(
                        AiIntent.GENERAL,
                        request
                );


        context.put(
                "supportedTopics",
                List.of(
                        "INVENTORY",
                        "SALES",
                        "PRODUCT",
                        "PURCHASE_ORDER",
                        "SUPPLIER",
                        "STOCK_TRANSFER",
                        "FORECAST"
                )
        );


        context.put(
                "message",
                "Câu hỏi chưa xác định được nhóm analytics cụ thể."
        );


        return context;
    }


    // =========================================================
    // BASE CONTEXT
    //
    // Luôn giữ câu hỏi và filter gốc để AI biết người dùng
    // đã yêu cầu gì.
    // =========================================================

    private Map<String, Object> createBaseContext(

            AiIntent intent,

            AiChatRequest request
    ) {

        Map<String, Object> context =
                new LinkedHashMap<>();


        context.put(
                "intent",
                intent.name()
        );


        context.put(
                "question",
                request.getMessage()
        );


        context.put(
                "requestedStoreId",
                request.getStoreId()
        );


        context.put(
                "requestedWarehouseId",
                request.getWarehouseId()
        );


        context.put(
                "requestedProductId",
                request.getProductId()
        );


        return context;
    }


    // =========================================================
    // WAREHOUSE RESOLUTION
    //
    // Trong ToyStorageSystem:
    //
    // STORE cũng được quản lý trong warehouses.
    //
    // Nếu caller truyền warehouseId:
    //     ưu tiên warehouseId.
    //
    // Nếu không có warehouseId nhưng có storeId:
    //     dùng storeId cho inventory analytics.
    // =========================================================

    private Long resolveWarehouseId(
            AiChatRequest request
    ) {

        if (
                request.getWarehouseId() != null
        ) {

            return request.getWarehouseId();
        }


        return request.getStoreId();
    }
}