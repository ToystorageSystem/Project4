
package com.toystorage.backend.controllers.analytics;

import com.toystorage.backend.dto.response.analytics.CategorySalesResponse;
import com.toystorage.backend.dto.response.analytics.ForecastAnalyticsResponse;
import com.toystorage.backend.dto.response.analytics.InventoryAnomalyResponse;
import com.toystorage.backend.dto.response.analytics.InventoryDashboardResponse;
import com.toystorage.backend.dto.response.analytics.InventoryMovementResponse;
import com.toystorage.backend.dto.response.analytics.InventoryTransferRecommendationResponse;
import com.toystorage.backend.dto.response.analytics.ProductPerformanceResponse;
import com.toystorage.backend.dto.response.analytics.PurchaseOrderAnalyticsResponse;
import com.toystorage.backend.dto.response.analytics.SalesDashboardResponse;
import com.toystorage.backend.dto.response.analytics.SalesTrendPointResponse;
import com.toystorage.backend.dto.response.analytics.SlowMovingInventoryResponse;
import com.toystorage.backend.dto.response.analytics.StockTransferOperationalAnalyticsResponse;
import com.toystorage.backend.dto.response.analytics.SupplierPerformanceAnalyticsResponse;

import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;

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

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics/dashboard")
@RequiredArgsConstructor

 @PreAuthorize("hasAnyAuthority('ROLE_ROLE-BIZ-OPS', 'ROLE_ROLE-BIZ-MANAGER', 'ROLE_ROLE-BIZ-STAFF', 'ROLE_BIZ_MANAGER', 'ROLE_BIZ_STAFF')")

public class AnalyticsDashboardController {

    private final InventoryAnalyticsService inventoryAnalyticsService;
    private final SalesAnalyticsService salesAnalyticsService;
    private final ProductAnalyticsService productAnalyticsService;
    private final CategoryAnalyticsService categoryAnalyticsService;
    private final InventoryMovementAnalyticsService inventoryMovementAnalyticsService;
    private final SlowMovingInventoryAnalyticsService slowMovingInventoryAnalyticsService;
    private final InventoryTransferRecommendationService inventoryTransferRecommendationService;
    private final InventoryAnomalyAnalyticsService inventoryAnomalyAnalyticsService;
    private final PurchaseOrderAnalyticsService purchaseOrderAnalyticsService;
    private final SupplierPerformanceAnalyticsService supplierPerformanceAnalyticsService;
    private final StockTransferOperationalAnalyticsService stockTransferOperationalAnalyticsService;
    private final ForecastAnalyticsService forecastAnalyticsService;

    /**
     * Cho phep Admin, Business Manager va Business Staff
     * xem du lieu phan tich toan he thong.
     */

    // 1. INVENTORY SUMMARY
    @GetMapping("/inventory")
    public ResponseEntity<InventoryDashboardResponse> getInventorySummary(
            @RequestParam(required = false) Long warehouseId
    ) {
        InventoryDashboardResponse response =
                inventoryAnalyticsService.getInventorySummary(warehouseId);

        return ResponseEntity.ok(response);
    }

    // 2. INVENTORY MOVEMENT
    @GetMapping("/inventory/movement")
    public ResponseEntity<List<InventoryMovementResponse>> getInventoryMovement(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<InventoryMovementResponse> response =
                inventoryMovementAnalyticsService.getInventoryMovement(
                        warehouseId, productId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 3. SLOW-MOVING INVENTORY
    @GetMapping("/inventory/slow-moving")
    public ResponseEntity<List<SlowMovingInventoryResponse>> getSlowMovingInventory(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<SlowMovingInventoryResponse> response =
                slowMovingInventoryAnalyticsService.getSlowMovingInventory(
                        warehouseId, productId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 4. INVENTORY TRANSFER RECOMMENDATIONS
    @GetMapping("/inventory/transfer-recommendations")
    public ResponseEntity<List<InventoryTransferRecommendationResponse>>
    getInventoryTransferRecommendations(
            @RequestParam(required = false) Long productId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<InventoryTransferRecommendationResponse> response =
                inventoryTransferRecommendationService.getTransferRecommendations(
                        productId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 5. INVENTORY ANOMALIES
    @GetMapping("/inventory/anomalies")
    public ResponseEntity<List<InventoryAnomalyResponse>> getInventoryAnomalies(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId
    ) {
        List<InventoryAnomalyResponse> response =
                inventoryAnomalyAnalyticsService.getInventoryAnomalies(
                        warehouseId, productId
                );

        return ResponseEntity.ok(response);
    }

    // 6. PURCHASE ORDER ANALYTICS
    @GetMapping("/purchase-orders")
    public ResponseEntity<List<PurchaseOrderAnalyticsResponse>>
    getPurchaseOrderAnalytics(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) PurchaseOrderStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<PurchaseOrderAnalyticsResponse> response =
                purchaseOrderAnalyticsService.getPurchaseOrderAnalytics(
                        supplierId, warehouseId, status, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 7. SUPPLIER PERFORMANCE
    @GetMapping("/suppliers/performance")
    public ResponseEntity<List<SupplierPerformanceAnalyticsResponse>>
    getSupplierPerformanceAnalytics(
            @RequestParam(required = false) Long supplierId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<SupplierPerformanceAnalyticsResponse> response =
                supplierPerformanceAnalyticsService.getSupplierPerformanceAnalytics(
                        supplierId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 8. STOCK TRANSFER OPERATIONS
    @GetMapping("/transfers/operations")
    public ResponseEntity<List<StockTransferOperationalAnalyticsResponse>>
    getStockTransferOperationalAnalytics(
            @RequestParam(required = false) Long fromWarehouseId,
            @RequestParam(required = false) Long toWarehouseId,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) TransferType transferType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<StockTransferOperationalAnalyticsResponse> response =
                stockTransferOperationalAnalyticsService.getOperationalAnalytics(
                        fromWarehouseId,
                        toWarehouseId,
                        status,
                        transferType,
                        fromDate,
                        toDate
                );

        return ResponseEntity.ok(response);
    }

    // 9. FORECAST ANALYTICS
    @GetMapping("/forecast")
    public ResponseEntity<List<ForecastAnalyticsResponse>> getForecastAnalytics(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long productId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,

            @RequestParam(required = false)
            Integer forecastPeriodDays
    ) {
        List<ForecastAnalyticsResponse> response =
                forecastAnalyticsService.getForecastAnalytics(
                        storeId,
                        productId,
                        fromDate,
                        toDate,
                        forecastPeriodDays
                );

        return ResponseEntity.ok(response);
    }

    // 10. SALES SUMMARY
    @GetMapping("/sales")
    public ResponseEntity<SalesDashboardResponse> getSalesSummary(
            @RequestParam(required = false) Long storeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        SalesDashboardResponse response =
                salesAnalyticsService.getSalesSummary(
                        storeId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 11. DAILY SALES TREND
    @GetMapping("/sales/trend/daily")
    public ResponseEntity<List<SalesTrendPointResponse>> getDailySalesTrend(
            @RequestParam(required = false) Long storeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<SalesTrendPointResponse> response =
                salesAnalyticsService.getDailySalesTrend(
                        storeId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 12. MONTHLY SALES TREND
    @GetMapping("/sales/trend/monthly")
    public ResponseEntity<List<SalesTrendPointResponse>> getMonthlySalesTrend(
            @RequestParam(required = false) Long storeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<SalesTrendPointResponse> response =
                salesAnalyticsService.getMonthlySalesTrend(
                        storeId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 13. PRODUCT PERFORMANCE
    @GetMapping("/products/performance")
    public ResponseEntity<List<ProductPerformanceResponse>> getProductPerformance(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long categoryId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<ProductPerformanceResponse> response =
                productAnalyticsService.getProductPerformance(
                        storeId, categoryId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }

    // 14. SALES BY CATEGORY
    @GetMapping("/categories/sales")
    public ResponseEntity<List<CategorySalesResponse>> getCategorySales(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long categoryId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        List<CategorySalesResponse> response =
                categoryAnalyticsService.getCategorySales(
                        storeId, categoryId, fromDate, toDate
                );

        return ResponseEntity.ok(response);
    }
}
