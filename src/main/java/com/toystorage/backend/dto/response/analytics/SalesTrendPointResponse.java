package com.toystorage.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendPointResponse {

    /**
     * Nhãn thời gian dùng cho biểu đồ.
     *
     * DAILY:
     * 2026-10-18
     *
     * MONTHLY:
     * 2026-10
     */
    private String period;

    /**
     * Doanh thu từ các đơn COMPLETED.
     */
    private BigDecimal revenue;

    /**
     * Số đơn COMPLETED.
     */
    private Long completedOrders;

    /**
     * Tổng số lượng sản phẩm đã bán
     * trong các đơn COMPLETED.
     */
    private Long soldQuantity;
}