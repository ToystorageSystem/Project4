package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.InventoryDashboardResponse;
import com.toystorage.backend.repository.analytics.InventoryAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAnalyticsService {

    private final InventoryAnalyticsRepository inventoryAnalyticsRepository;

    /**
     * Lấy KPI tồn kho.
     *
     * warehouseId == null:
     *      phân tích toàn hệ thống.
     *
     * warehouseId != null:
     *      phân tích riêng một kho/cửa hàng.
     */
    public InventoryDashboardResponse getInventorySummary(Long warehouseId) {

        Long totalQuantity;
        Long totalReservedQuantity;
        Long totalAvailableQuantity;

        if (warehouseId == null) {

            totalQuantity =
                    inventoryAnalyticsRepository.getTotalQuantity();

            totalReservedQuantity =
                    inventoryAnalyticsRepository.getTotalReservedQuantity();

            totalAvailableQuantity =
                    inventoryAnalyticsRepository.getTotalAvailableQuantity();

        } else {

            totalQuantity =
                    inventoryAnalyticsRepository
                            .getTotalQuantityByWarehouse(warehouseId);

            totalReservedQuantity =
                    inventoryAnalyticsRepository
                            .getTotalReservedQuantityByWarehouse(warehouseId);

            totalAvailableQuantity =
                    inventoryAnalyticsRepository
                            .getTotalAvailableQuantityByWarehouse(warehouseId);
        }

        return InventoryDashboardResponse.builder()
                .warehouseId(warehouseId)
                .totalQuantity(safeLong(totalQuantity))
                .totalReservedQuantity(safeLong(totalReservedQuantity))
                .totalAvailableQuantity(safeLong(totalAvailableQuantity))
                .build();
    }

    /**
     * Bảo đảm API luôn trả về số,
     * không trả null khi database chưa có dữ liệu.
     */
    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}