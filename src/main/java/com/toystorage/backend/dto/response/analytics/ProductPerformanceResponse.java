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
public class ProductPerformanceResponse {

    // =========================================================
    // PRODUCT INFORMATION
    // =========================================================

    private Long productId;

    private String productCode;

    private String productName;

    private String baseUnit;

    private BigDecimal sellingPrice;


    // =========================================================
    // CATEGORY INFORMATION
    // =========================================================

    private Long categoryId;

    private String categoryCode;

    private String categoryName;


    // =========================================================
    // SALES PERFORMANCE
    // =========================================================

    /**
     * Tổng số lượng sản phẩm đã bán
     * trong các đơn COMPLETED.
     */
    private Long soldQuantity;

    /**
     * Tổng doanh thu của sản phẩm
     * trong các đơn COMPLETED.
     */
    private BigDecimal revenue;

    /**
     * Số đơn COMPLETED có chứa sản phẩm.
     */
    private Long completedOrderCount;


    // =========================================================
    // ANALYTICS RESULT
    // =========================================================

    /**
     * Phân loại hiệu suất:
     *
     * BEST_SELLING
     * NORMAL
     * SLOW_SELLING
     * NO_SALES
     */
    private String performanceType;

    /**
     * Nhận xét hoặc đề xuất hỗ trợ quyết định.
     *
     * Ví dụ:
     *
     * - Đề xuất tăng nhập sản phẩm.
     * - Theo dõi thêm doanh số.
     * - Cân nhắc giảm số lượng nhập.
     * - Sản phẩm chưa phát sinh doanh số.
     */
    private String recommendation;
}