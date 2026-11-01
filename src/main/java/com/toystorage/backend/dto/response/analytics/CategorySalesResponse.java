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
public class CategorySalesResponse {

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
     * Tổng doanh thu của danh mục
     * trong các đơn COMPLETED.
     */
    private BigDecimal revenue;

    /**
     * Số đơn COMPLETED có chứa ít nhất
     * một sản phẩm thuộc danh mục.
     */
    private Long completedOrderCount;


    // =========================================================
    // REVENUE SHARE
    // =========================================================

    /**
     * Tỷ trọng doanh thu của danh mục
     * trên tổng doanh thu của tất cả danh mục.
     *
     * Ví dụ:
     *
     * Dog Food = 75.50
     * Pet Care = 24.50
     *
     * Đơn vị: phần trăm.
     */
    private BigDecimal revenueSharePercent;
}