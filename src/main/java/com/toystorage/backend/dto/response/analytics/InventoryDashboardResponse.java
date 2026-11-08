package com.toystorage.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDashboardResponse {

    /**
     * Warehouse được lọc.
     *
     * null = toàn hệ thống.
     */
    private Long warehouseId;

    /**
     * Tổng số lượng vật lý hiện có.
     */
    private Long totalQuantity;

    /**
     * Tổng số lượng đang được giữ chỗ.
     */
    private Long totalReservedQuantity;

    /**
     * Tổng số lượng có thể sử dụng.
     *
     * available = quantity - reserved
     */
    private Long totalAvailableQuantity;
}