package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.CategorySalesResponse;

import com.toystorage.backend.entity.products.Categories;

import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.stores.SalesOrderStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.analytics.CategoryAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAnalyticsService {

    private final CategoryAnalyticsRepository
            categoryAnalyticsRepository;


    // =========================================================
    // CATEGORY SALES
    // =========================================================

    public List<CategorySalesResponse> getCategorySales(

            Long storeId,

            Long categoryId,

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        // =====================================================
        // 1. GET ACTIVE CATEGORIES
        // =====================================================

        List<Categories> categories =
                categoryAnalyticsRepository
                        .findCategoriesForAnalytics(

                                CommonStatus.ACTIVE,

                                categoryId
                        );


        // =====================================================
        // 2. GET COMPLETED SALES BY CATEGORY
        // =====================================================

        List<Object[]> salesRows =
                categoryAnalyticsRepository
                        .findCategorySalesAggregates(

                                SalesOrderStatus.COMPLETED,

                                storeId,

                                categoryId,

                                fromDate,

                                toDate
                        );


        // =====================================================
        // 3. BUILD SALES MAP
        //
        // Key:
        //      categoryId
        //
        // Value:
        //      soldQuantity
        //      revenue
        //      completedOrderCount
        // =====================================================

        Map<Long, CategorySalesData> salesByCategoryId =
                buildSalesMap(
                        salesRows
                );


        // =====================================================
        // 4. CALCULATE TOTAL REVENUE
        //
        // Dùng để tính tỷ trọng doanh thu.
        // =====================================================

        BigDecimal totalRevenue =
                calculateTotalRevenue(

                        categories,

                        salesByCategoryId
                );


        // =====================================================
        // 5. BUILD RESPONSE
        // =====================================================

        List<CategorySalesResponse> result =
                new ArrayList<>();


        for (Categories category : categories) {

            CategorySalesData salesData =
                    salesByCategoryId.getOrDefault(

                            category.getId(),

                            CategorySalesData.empty()
                    );


            BigDecimal revenueSharePercent =
                    calculateRevenueSharePercent(

                            salesData.revenue,

                            totalRevenue
                    );


            result.add(

                    CategorySalesResponse
                            .builder()

                            .categoryId(
                                    category.getId()
                            )

                            .categoryCode(
                                    category.getCategoriesCode()
                            )

                            .categoryName(
                                    category.getName()
                            )

                            .soldQuantity(
                                    salesData.soldQuantity
                            )

                            .revenue(
                                    salesData.revenue
                            )

                            .completedOrderCount(
                                    salesData.completedOrderCount
                            )

                            .revenueSharePercent(
                                    revenueSharePercent
                            )

                            .build()
            );
        }


        // =====================================================
        // 6. SORT
        //
        // Ưu tiên:
        //
        // - doanh thu giảm dần
        // - số lượng bán giảm dần
        // - tên danh mục tăng dần
        // =====================================================

        result.sort(

                Comparator

                        .comparing(
                                CategorySalesResponse::getRevenue,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                CategorySalesResponse::getSoldQuantity,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                CategorySalesResponse::getCategoryName,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
        );


        return result;
    }


    // =========================================================
    // BUILD SALES MAP
    // =========================================================

    private Map<Long, CategorySalesData> buildSalesMap(
            List<Object[]> rows
    ) {

        Map<Long, CategorySalesData> result =
                new HashMap<>();


        if (rows == null) {
            return result;
        }


        for (Object[] row : rows) {

            if (
                    row == null
                            || row.length < 4
                            || row[0] == null
            ) {

                continue;
            }


            Long categoryId =
                    toLong(
                            row[0]
                    );


            Long soldQuantity =
                    toLong(
                            row[1]
                    );


            BigDecimal revenue =
                    toBigDecimal(
                            row[2]
                    );


            Long completedOrderCount =
                    toLong(
                            row[3]
                    );


            result.put(

                    categoryId,

                    new CategorySalesData(

                            soldQuantity,

                            revenue,

                            completedOrderCount
                    )
            );
        }


        return result;
    }


    // =========================================================
    // TOTAL REVENUE
    // =========================================================

    private BigDecimal calculateTotalRevenue(

            List<Categories> categories,

            Map<Long, CategorySalesData> salesByCategoryId
    ) {

        BigDecimal totalRevenue =
                BigDecimal.ZERO;


        if (categories == null) {
            return totalRevenue;
        }


        for (Categories category : categories) {

            CategorySalesData salesData =
                    salesByCategoryId.getOrDefault(

                            category.getId(),

                            CategorySalesData.empty()
                    );


            totalRevenue =
                    totalRevenue.add(
                            safeBigDecimal(
                                    salesData.revenue
                            )
                    );
        }


        return totalRevenue;
    }


    // =========================================================
    // REVENUE SHARE
    //
    // categoryRevenue / totalRevenue * 100
    // =========================================================

    private BigDecimal calculateRevenueSharePercent(

            BigDecimal categoryRevenue,

            BigDecimal totalRevenue
    ) {

        BigDecimal safeCategoryRevenue =
                safeBigDecimal(
                        categoryRevenue
                );


        BigDecimal safeTotalRevenue =
                safeBigDecimal(
                        totalRevenue
                );


        if (
                safeTotalRevenue.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {

            return BigDecimal.ZERO
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }


        return safeCategoryRevenue

                .multiply(
                        BigDecimal.valueOf(
                                100
                        )
                )

                .divide(
                        safeTotalRevenue,
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // VALIDATE DATE RANGE
    // =========================================================

    private void validateDateRange(

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        if (
                fromDate != null
                        && toDate != null
                        && fromDate.isAfter(
                                toDate
                        )
        ) {

            throw new BadRequest(
                    "Thời gian bắt đầu không được sau thời gian kết thúc"
            );
        }
    }


    // =========================================================
    // SAFE CONVERSION
    // =========================================================

    private Long toLong(
            Object value
    ) {

        if (value == null) {
            return 0L;
        }


        if (value instanceof Number number) {

            return number.longValue();
        }


        return Long.parseLong(
                value.toString()
        );
    }


    private BigDecimal toBigDecimal(
            Object value
    ) {

        if (value == null) {
            return BigDecimal.ZERO;
        }


        if (value instanceof BigDecimal bigDecimal) {

            return bigDecimal;
        }


        return new BigDecimal(
                value.toString()
        );
    }


    private BigDecimal safeBigDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    // =========================================================
    // INTERNAL DATA
    // =========================================================

    private static class CategorySalesData {

        private final Long soldQuantity;

        private final BigDecimal revenue;

        private final Long completedOrderCount;


        private CategorySalesData(

                Long soldQuantity,

                BigDecimal revenue,

                Long completedOrderCount
        ) {

            this.soldQuantity =
                    soldQuantity == null
                            ? 0L
                            : soldQuantity;


            this.revenue =
                    revenue == null
                            ? BigDecimal.ZERO
                            : revenue;


            this.completedOrderCount =
                    completedOrderCount == null
                            ? 0L
                            : completedOrderCount;
        }


        private static CategorySalesData empty() {

            return new CategorySalesData(

                    0L,

                    BigDecimal.ZERO,

                    0L
            );
        }
    }
}