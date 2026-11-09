package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.ProductPerformanceResponse;

import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.Products;

import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.stores.SalesOrderStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.analytics.ProductAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductAnalyticsService {

    private final ProductAnalyticsRepository
            productAnalyticsRepository;


    // =========================================================
    // PRODUCT PERFORMANCE
    // =========================================================

    public List<ProductPerformanceResponse> getProductPerformance(

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
        // 1. GET ACTIVE PRODUCTS
        // =====================================================

        List<Products> products =
                productAnalyticsRepository
                        .findProductsForAnalytics(

                                ProductStatus.ACTIVE,

                                categoryId
                        );


        // =====================================================
        // 2. GET COMPLETED SALES AGGREGATES
        // =====================================================

        List<Object[]> salesRows =
                productAnalyticsRepository
                        .findProductSalesAggregates(

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
        //      productId
        //
        // Value:
        //      soldQuantity
        //      revenue
        //      completedOrderCount
        // =====================================================

        Map<Long, ProductSalesData> salesByProductId =
                buildSalesMap(
                        salesRows
                );


        // =====================================================
        // 4. CALCULATE CLASSIFICATION BASE
        // =====================================================

        SalesClassificationBase classificationBase =
                calculateClassificationBase(

                        products,

                        salesByProductId
                );


        // =====================================================
        // 5. BUILD RESPONSE
        // =====================================================

        List<ProductPerformanceResponse> result =
                new ArrayList<>();


        for (Products product : products) {

            ProductSalesData salesData =
                    salesByProductId.getOrDefault(

                            product.getId(),

                            ProductSalesData.empty()
                    );


            String performanceType =
                    classifyPerformance(

                            salesData.soldQuantity,

                            classificationBase
                    );


            String recommendation =
                    buildRecommendation(

                            performanceType,

                            salesData.soldQuantity
                    );


            Categories category =
                    product.getCategory();


            result.add(

                    ProductPerformanceResponse
                            .builder()

                            // =================================
                            // PRODUCT
                            // =================================

                            .productId(
                                    product.getId()
                            )

                            .productCode(
                                    product.getProductsCode()
                            )

                            .productName(
                                    product.getName()
                            )

                            .baseUnit(
                                    product.getBaseUnit()
                            )

                            .sellingPrice(
                                    safeBigDecimal(
                                            product.getSellingPrice()
                                    )
                            )


                            // =================================
                            // CATEGORY
                            // =================================

                            .categoryId(
                                    category != null
                                            ? category.getId()
                                            : null
                            )

                            .categoryCode(
                                    category != null
                                            ? category.getCategoriesCode()
                                            : null
                            )

                            .categoryName(
                                    category != null
                                            ? category.getName()
                                            : null
                            )


                            // =================================
                            // SALES
                            // =================================

                            .soldQuantity(
                                    salesData.soldQuantity
                            )

                            .revenue(
                                    salesData.revenue
                            )

                            .completedOrderCount(
                                    salesData.completedOrderCount
                            )


                            // =================================
                            // ANALYTICS
                            // =================================

                            .performanceType(
                                    performanceType
                            )

                            .recommendation(
                                    recommendation
                            )

                            .build()
            );
        }


        // =====================================================
        // 6. SORT
        //
        // Ưu tiên:
        //
        // - số lượng bán giảm dần
        // - doanh thu giảm dần
        // - tên sản phẩm tăng dần
        // =====================================================

        result.sort(

                Comparator

                        .comparing(
                                ProductPerformanceResponse::getSoldQuantity,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                ProductPerformanceResponse::getRevenue,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                ProductPerformanceResponse::getProductName,
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

    private Map<Long, ProductSalesData> buildSalesMap(
            List<Object[]> rows
    ) {

        Map<Long, ProductSalesData> result =
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


            Long productId =
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

                    productId,

                    new ProductSalesData(

                            soldQuantity,

                            revenue,

                            completedOrderCount
                    )
            );
        }


        return result;
    }


    // =========================================================
    // CLASSIFICATION BASE
    //
    // Chỉ tính các sản phẩm có doanh số > 0.
    // =========================================================

    private SalesClassificationBase calculateClassificationBase(

            List<Products> products,

            Map<Long, ProductSalesData> salesByProductId
    ) {

        long maxSoldQuantity = 0L;

        long totalPositiveSoldQuantity = 0L;

        long positiveSalesProductCount = 0L;


        for (Products product : products) {

            ProductSalesData salesData =
                    salesByProductId.getOrDefault(

                            product.getId(),

                            ProductSalesData.empty()
                    );


            long soldQuantity =
                    safeLong(
                            salesData.soldQuantity
                    );


            if (soldQuantity <= 0L) {
                continue;
            }


            positiveSalesProductCount++;


            totalPositiveSoldQuantity +=
                    soldQuantity;


            if (soldQuantity > maxSoldQuantity) {

                maxSoldQuantity =
                        soldQuantity;
            }
        }


        return new SalesClassificationBase(

                maxSoldQuantity,

                totalPositiveSoldQuantity,

                positiveSalesProductCount
        );
    }


       // =========================================================
    // CLASSIFY PERFORMANCE
    // PHÂN LOẠI HIỆU SUẤT
    //
    // ENGLISH NOTE:
    // Classify product sales performance based on actual
    // sales data in the analyzed dataset.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Phân loại hiệu suất bán hàng của sản phẩm dựa trên
    // dữ liệu bán hàng thực tế trong phạm vi phân tích.
    // =========================================================

    private String classifyPerformance(

            Long soldQuantityValue,

            SalesClassificationBase base
    ) {

        long soldQuantity =
                safeLong(
                        soldQuantityValue
                );


        // -----------------------------------------------------
        // NO SALES
        // KHÔNG CÓ DOANH SỐ
        //
        // ENGLISH NOTE:
        // The product has no completed sales.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Sản phẩm chưa phát sinh lượt bán hoàn tất.
        // -----------------------------------------------------

        if (soldQuantity == 0L) {

            return "NO_SALES";
        }


        // -----------------------------------------------------
        // BEST SELLING
        // BÁN CHẠY NHẤT
        //
        // ENGLISH NOTE:
        // The product has the highest sold quantity.
        // Multiple products may share the same highest value.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Sản phẩm có số lượng bán cao nhất.
        // Có thể có nhiều sản phẩm đồng hạng.
        // -----------------------------------------------------

        if (
                base.maxSoldQuantity > 0L
                        && soldQuantity
                        == base.maxSoldQuantity
        ) {

            return "BEST_SELLING";
        }


        // -----------------------------------------------------
        // SLOW SELLING
        // BÁN CHẬM
        //
        // ENGLISH NOTE:
        // soldQuantity is lower than the average positive
        // sold quantity of products with sales.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // soldQuantity thấp hơn mức bán trung bình
        // của các sản phẩm có doanh số.
        //
        // ENGLISH NOTE:
        // Avoid floating-point division by comparing:
        //
        // soldQuantity * count < total
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Không dùng phép chia số thực, thay vào đó so sánh:
        //
        // soldQuantity * count < total
        // -----------------------------------------------------

        if (
                base.positiveSalesProductCount > 0L
                        && soldQuantity
                        * base.positiveSalesProductCount
                        < base.totalPositiveSoldQuantity
        ) {

            return "SLOW_SELLING";
        }


        // ENGLISH NOTE:
        // The product is neither best-selling nor slow-selling.
        //
        // GHI CHÚ TIẾNG VIỆT:
        // Sản phẩm không thuộc nhóm bán chạy nhất
        // cũng không thuộc nhóm bán chậm.

        return "NORMAL";
    }


    // =========================================================
    // RECOMMENDATION
    // ĐỀ XUẤT
    //
    // ENGLISH NOTE:
    // All recommendation text returned to the frontend
    // must be in English.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Toàn bộ recommendation trả về frontend
    // phải sử dụng tiếng Anh.
    // =========================================================

    private String buildRecommendation(

            String performanceType,

            Long soldQuantity
    ) {

        return switch (performanceType) {

            // =================================================
            // BEST SELLING
            // BÁN CHẠY NHẤT
            // =================================================

            case "BEST_SELLING" ->

                    "The product is currently performing well in sales. "
                            + "Monitor inventory levels and consider increasing "
                            + "replenishment if demand remains stable.";


            // =================================================
            // SLOW SELLING
            // BÁN CHẬM
            // =================================================

            case "SLOW_SELLING" ->

                    "The product is selling below the average rate. "
                            + "Consider reducing replenishment quantities, "
                            + "adjusting sales strategies, or continuing "
                            + "to monitor performance in the next period.";


            // =================================================
            // NO SALES
            // KHÔNG CÓ DOANH SỐ
            // =================================================

            case "NO_SALES" ->

                    "The product has not generated any sales "
                            + "during the analyzed period. "
                            + "Review market demand, selling price, "
                            + "and replenishment plans.";


            // =================================================
            // NORMAL
            // BÌNH THƯỜNG
            // =================================================

            default ->

                    "The product currently has a stable sales level. "
                            + "Continue monitoring sales performance "
                            + "and inventory levels.";
        };
    }


    // =========================================================
    // VALIDATE DATE RANGE
    // KIỂM TRA KHOẢNG THỜI GIAN
    //
    // ENGLISH NOTE:
    // The start date cannot be later than the end date.
    //
    // GHI CHÚ TIẾNG VIỆT:
    // Thời gian bắt đầu không được sau thời gian kết thúc.
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
                    "The start date must not be later than the end date."
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


    private Long safeLong(
            Long value
    ) {

        return value == null
                ? 0L
                : value;
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

    private static class ProductSalesData {

        private final Long soldQuantity;

        private final BigDecimal revenue;

        private final Long completedOrderCount;


        private ProductSalesData(

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


        private static ProductSalesData empty() {

            return new ProductSalesData(

                    0L,

                    BigDecimal.ZERO,

                    0L
            );
        }
    }


    private static class SalesClassificationBase {

        private final long maxSoldQuantity;

        private final long totalPositiveSoldQuantity;

        private final long positiveSalesProductCount;


        private SalesClassificationBase(

                long maxSoldQuantity,

                long totalPositiveSoldQuantity,

                long positiveSalesProductCount
        ) {

            this.maxSoldQuantity =
                    maxSoldQuantity;


            this.totalPositiveSoldQuantity =
                    totalPositiveSoldQuantity;


            this.positiveSalesProductCount =
                    positiveSalesProductCount;
        }
    }
}
