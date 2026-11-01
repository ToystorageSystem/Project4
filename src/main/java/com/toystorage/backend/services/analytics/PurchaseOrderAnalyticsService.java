package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.PurchaseOrderAnalyticsResponse;

import com.toystorage.backend.entity.receipts.PurchaseOrderItems;
import com.toystorage.backend.entity.receipts.PurchaseOrders;

import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.analytics.PurchaseOrderAnalyticsRepository;
import com.toystorage.backend.repository.analytics.PurchaseOrderItemAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderAnalyticsService {

    private final PurchaseOrderAnalyticsRepository
            purchaseOrderAnalyticsRepository;

    private final PurchaseOrderItemAnalyticsRepository
            purchaseOrderItemAnalyticsRepository;


    // =========================================================
    // PURCHASE ORDER ANALYTICS
    //
    // Phân tích:
    //
    // - tổng sản phẩm
    // - tổng số lượng đặt
    // - tổng số lượng NCC xác nhận
    // - tổng số lượng đã nhận
    // - tổng giá trị đặt hàng
    // - tổng giá trị NCC xác nhận
    // - tiến độ nhận hàng
    // - trạng thái giao hàng
    // - số ngày quá hạn
    // - phản hồi NCC
    // - recommendation
    //
    // Filter:
    //
    // - supplier
    // - warehouse
    // - PO status
    // - khoảng thời gian tạo PO
    //
    // Chỉ phân tích.
    // KHÔNG thay đổi Purchase Order.
    // =========================================================

    public List<PurchaseOrderAnalyticsResponse>
    getPurchaseOrderAnalytics(

            Long supplierId,

            Long warehouseId,

            PurchaseOrderStatus status,

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        // =====================================================
        // 1. VALIDATE DATE RANGE
        // =====================================================

        validateDateRange(
                fromDate,
                toDate
        );


        // =====================================================
        // 2. LOAD PURCHASE ORDERS
        // =====================================================

        List<PurchaseOrders> orders =
                purchaseOrderAnalyticsRepository
                        .findForAnalytics(
                                supplierId,
                                warehouseId,
                                status,
                                fromDate,
                                toDate
                        );


        if (
                orders == null
                        || orders.isEmpty()
        ) {

            return new ArrayList<>();
        }


        // =====================================================
        // 3. COLLECT PURCHASE ORDER IDS
        // =====================================================

        List<Long> purchaseOrderIds =
                orders.stream()
                        .map(
                                PurchaseOrders::getId
                        )
                        .toList();


        // =====================================================
        // 4. LOAD ALL PURCHASE ORDER ITEMS
        //
        // Dùng repository analytics riêng.
        // Không phụ thuộc repository cũ của Issue khác.
        // =====================================================

        List<PurchaseOrderItems> items =
                purchaseOrderItemAnalyticsRepository
                        .findAllByPurchaseOrderIds(
                                purchaseOrderIds
                        );


        // =====================================================
        // 5. GROUP ITEMS BY PURCHASE ORDER
        // =====================================================

        Map<Long, List<PurchaseOrderItems>> itemMap =
                groupItemsByPurchaseOrder(
                        items
                );


        // =====================================================
        // 6. BUILD ANALYTICS
        // =====================================================

        LocalDate currentDate =
                LocalDate.now();


        List<PurchaseOrderAnalyticsResponse> result =
                new ArrayList<>();


        for (PurchaseOrders order : orders) {

            List<PurchaseOrderItems> orderItems =
                    itemMap.getOrDefault(
                            order.getId(),
                            new ArrayList<>()
                    );


            PurchaseOrderAnalyticsResponse response =
                    buildResponse(
                            order,
                            orderItems,
                            currentDate
                    );


            result.add(
                    response
            );
        }


        return result;
    }


    // =========================================================
    // BUILD RESPONSE
    // =========================================================

    private PurchaseOrderAnalyticsResponse buildResponse(

            PurchaseOrders order,

            List<PurchaseOrderItems> items,

            LocalDate currentDate
    ) {

        // =====================================================
        // SUMMARY
        // =====================================================

        long totalProducts =
                items.size();


        long totalOrderedQuantity =
                0L;


        long totalConfirmedQuantity =
                0L;


        long totalReceivedQuantity =
                0L;


        BigDecimal totalOrderedAmount =
                BigDecimal.ZERO;


        BigDecimal totalConfirmedAmount =
                BigDecimal.ZERO;


        // =====================================================
        // CALCULATE PURCHASE ORDER ITEMS
        // =====================================================

        for (PurchaseOrderItems item : items) {

            if (
                    item == null
            ) {

                continue;
            }


            long orderedQuantity =
                    safeLong(
                            item.getOrderedQuantity()
                    );


            long confirmedQuantity =
                    safeLong(
                            item.getConfirmedQuantity()
                    );


            long receivedQuantity =
                    safeLong(
                            item.getReceivedQuantity()
                    );


            BigDecimal unitPrice =
                    safeBigDecimal(
                            item.getUnitPrice()
                    );


            BigDecimal confirmedUnitPrice =
                    item.getConfirmedUnitPrice();


            // =================================================
            // QUANTITY
            // =================================================

            totalOrderedQuantity +=
                    orderedQuantity;


            totalConfirmedQuantity +=
                    confirmedQuantity;


            totalReceivedQuantity +=
                    receivedQuantity;


            // =================================================
            // ORDERED AMOUNT
            //
            // orderedQuantity * unitPrice
            // =================================================

            BigDecimal orderedAmount =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    orderedQuantity
                            )
                    );


            totalOrderedAmount =
                    totalOrderedAmount.add(
                            orderedAmount
                    );


            // =================================================
            // CONFIRMED AMOUNT
            //
            // Chỉ tính khi NCC đã xác nhận:
            //
            // - confirmed quantity
            // - confirmed unit price
            // =================================================

            if (
                    item.getConfirmedQuantity() != null
                            && confirmedUnitPrice != null
            ) {

                BigDecimal confirmedAmount =
                        confirmedUnitPrice.multiply(
                                BigDecimal.valueOf(
                                        confirmedQuantity
                                )
                        );


                totalConfirmedAmount =
                        totalConfirmedAmount.add(
                                confirmedAmount
                        );
            }
        }


        // =====================================================
        // RECEIVING PROGRESS
        // =====================================================

        BigDecimal receivingProgressPercent =
                calculateReceivingProgress(
                        totalReceivedQuantity,
                        totalOrderedQuantity
                );


        // =====================================================
        // PURCHASE ORDER STATUS
        // =====================================================

        String orderStatus =
                order.getStatus() != null
                        ? order
                                .getStatus()
                                .name()
                        : null;


        // =====================================================
        // SUPPLIER RESPONSE
        // =====================================================

        String supplierResponseStatus =
                order.getSupplierResponseStatus() != null
                        ? order
                                .getSupplierResponseStatus()
                                .name()
                        : null;


        String supplierAnalysis =
                analyzeSupplierResponse(
                        supplierResponseStatus
                );


        // =====================================================
        // DELIVERY STATUS
        // =====================================================

        String deliveryStatus =
                determineDeliveryStatus(
                        orderStatus,
                        order.getExpectedDeliveryDate(),
                        currentDate
                );


        // =====================================================
        // OVERDUE DAYS
        // =====================================================

        long overdueDays =
                calculateOverdueDays(
                        deliveryStatus,
                        order.getExpectedDeliveryDate(),
                        currentDate
                );


        // =====================================================
        // RECOMMENDATION
        // =====================================================

        String recommendation =
                buildRecommendation(
                        orderStatus,
                        deliveryStatus,
                        overdueDays,
                        supplierAnalysis,
                        totalOrderedQuantity,
                        totalReceivedQuantity
                );


        // =====================================================
        // BUILD RESPONSE
        // =====================================================

        return PurchaseOrderAnalyticsResponse
                .builder()

                // =================================================
                // PURCHASE ORDER
                // =================================================

                .purchaseOrderId(
                        order.getId()
                )

                .orderCode(
                        order.getOrderCode()
                )

                .status(
                        orderStatus
                )


                // =================================================
                // SUPPLIER
                // =================================================

                .supplierId(
                        order.getSupplier() != null
                                ? order
                                        .getSupplier()
                                        .getId()
                                : null
                )

                .supplierName(
                        order.getSupplier() != null
                                ? order
                                        .getSupplier()
                                        .getName()
                                : null
                )

                .supplierResponseStatus(
                        supplierResponseStatus
                )

                .supplierRespondedAt(
                        order.getSupplierRespondedAt()
                )


                // =================================================
                // WAREHOUSE
                // =================================================

                .warehouseId(
                        order.getWarehouse() != null
                                ? order
                                        .getWarehouse()
                                        .getId()
                                : null
                )

                .warehouseName(
                        order.getWarehouse() != null
                                ? order
                                        .getWarehouse()
                                        .getName()
                                : null
                )


                // =================================================
                // DATE
                // =================================================

                .expectedDeliveryDate(
                        order.getExpectedDeliveryDate()
                )

                .createdAt(
                        order.getCreatedAt()
                )

                .updatedAt(
                        order.getUpdatedAt()
                )


                // =================================================
                // ORDER SUMMARY
                // =================================================

                .totalProducts(
                        totalProducts
                )

                .totalOrderedQuantity(
                        totalOrderedQuantity
                )

                .totalConfirmedQuantity(
                        totalConfirmedQuantity
                )

                .totalReceivedQuantity(
                        totalReceivedQuantity
                )


                // =================================================
                // VALUE
                // =================================================

                .totalOrderedAmount(
                        totalOrderedAmount
                )

                .totalConfirmedAmount(
                        totalConfirmedAmount
                )


                // =================================================
                // PROGRESS
                // =================================================

                .receivingProgressPercent(
                        receivingProgressPercent
                )


                // =================================================
                // ANALYTICS
                // =================================================

                .deliveryStatus(
                        deliveryStatus
                )

                .overdueDays(
                        overdueDays
                )

                .supplierAnalysis(
                        supplierAnalysis
                )

                .recommendation(
                        recommendation
                )

                .build();
    }


    // =========================================================
    // GROUP ITEMS BY PURCHASE ORDER
    // =========================================================

    private Map<Long, List<PurchaseOrderItems>>
    groupItemsByPurchaseOrder(

            List<PurchaseOrderItems> items
    ) {

        Map<Long, List<PurchaseOrderItems>> result =
                new HashMap<>();


        if (
                items == null
        ) {

            return result;
        }


        for (PurchaseOrderItems item : items) {

            if (
                    item == null
                            || item.getPurchaseOrder() == null
                            || item.getPurchaseOrder().getId() == null
            ) {

                continue;
            }


            Long purchaseOrderId =
                    item
                            .getPurchaseOrder()
                            .getId();


            result.computeIfAbsent(
                    purchaseOrderId,
                    ignored ->
                            new ArrayList<>()
            ).add(
                    item
            );
        }


        return result;
    }


    // =========================================================
    // RECEIVING PROGRESS
    // =========================================================

    private BigDecimal calculateReceivingProgress(

            long receivedQuantity,

            long orderedQuantity
    ) {

        if (
                orderedQuantity <= 0L
        ) {

            return BigDecimal.ZERO;
        }


        return BigDecimal
                .valueOf(
                        receivedQuantity
                )

                .multiply(
                        BigDecimal.valueOf(
                                100L
                        )
                )

                .divide(
                        BigDecimal.valueOf(
                                orderedQuantity
                        ),
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // =========================================================
    // DELIVERY STATUS
    //
    // Chỉ là analytics status.
    //
    // Không thay đổi PurchaseOrderStatus trong DB.
    // =========================================================

    private String determineDeliveryStatus(

            String orderStatus,

            LocalDate expectedDeliveryDate,

            LocalDate currentDate
    ) {

        // =====================================================
        // CANCELLED
        // =====================================================

        if (
                "CANCELLED".equals(
                        orderStatus
                )
        ) {

            return "CANCELLED";
        }


        // =====================================================
        // COMPLETED
        // =====================================================

        if (
                "COMPLETED".equals(
                        orderStatus
                )
        ) {

            return "COMPLETED";
        }


        // =====================================================
        // NO EXPECTED DATE
        // =====================================================

        if (
                expectedDeliveryDate == null
        ) {

            return "NO_EXPECTED_DATE";
        }


        // =====================================================
        // OVERDUE
        // =====================================================

        if (
                currentDate.isAfter(
                        expectedDeliveryDate
                )
        ) {

            return "OVERDUE";
        }


        // =====================================================
        // ON TIME
        // =====================================================

        return "ON_TIME";
    }


    // =========================================================
    // OVERDUE DAYS
    // =========================================================

    private long calculateOverdueDays(

            String deliveryStatus,

            LocalDate expectedDeliveryDate,

            LocalDate currentDate
    ) {

        if (
                !"OVERDUE".equals(
                        deliveryStatus
                )
                        || expectedDeliveryDate == null
        ) {

            return 0L;
        }


        return ChronoUnit.DAYS.between(
                expectedDeliveryDate,
                currentDate
        );
    }


    // =========================================================
    // SUPPLIER ANALYSIS
    // =========================================================

    private String analyzeSupplierResponse(
            String supplierResponseStatus
    ) {

        if (
                supplierResponseStatus == null
                        || supplierResponseStatus.isBlank()
                        || "PENDING_RESPONSE".equals(
                                supplierResponseStatus
                        )
        ) {

            return "WAITING_SUPPLIER_RESPONSE";
        }


        return switch (supplierResponseStatus) {

            case "AVAILABLE" ->
                    "SUPPLIER_AVAILABLE";

            case "DELAYED" ->
                    "SUPPLIER_DELAYED";

            case "DECLINED" ->
                    "SUPPLIER_DECLINED";

            case "PRICE_CHANGED" ->
                    "PRICE_CHANGED";

            case "PARTIALLY_AVAILABLE" ->
                    "PARTIALLY_AVAILABLE";

            case "OUT_OF_STOCK" ->
                    "OUT_OF_STOCK";

            case "DISCONTINUED" ->
                    "DISCONTINUED";

            default ->
                    supplierResponseStatus;
        };
    }


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    private String buildRecommendation(

            String orderStatus,

            String deliveryStatus,

            long overdueDays,

            String supplierAnalysis,

            long totalOrderedQuantity,

            long totalReceivedQuantity
    ) {

        // =====================================================
        // CANCELLED
        // =====================================================

        if (
                "CANCELLED".equals(
                        orderStatus
                )
        ) {

            return "Đơn mua hàng đã bị hủy. "
                    + "Không cần tiếp tục theo dõi nhận hàng.";
        }


        // =====================================================
        // COMPLETED
        // =====================================================

        if (
                "COMPLETED".equals(
                        orderStatus
                )
        ) {

            return "Đơn mua hàng đã hoàn tất. "
                    + "Có thể sử dụng dữ liệu này để đánh giá "
                    + "hiệu suất nhà cung cấp.";
        }


        // =====================================================
        // OVERDUE
        // =====================================================

        if (
                "OVERDUE".equals(
                        deliveryStatus
                )
        ) {

            return "Đơn mua hàng đã quá ngày giao dự kiến "
                    + overdueDays
                    + " ngày nhưng chưa hoàn tất. "
                    + "Cần kiểm tra tình trạng giao hàng và "
                    + "liên hệ nhà cung cấp.";
        }


        // =====================================================
        // DRAFT
        // =====================================================

        if (
                "DRAFT".equals(
                        orderStatus
                )
        ) {

            return "Đơn mua hàng đang ở trạng thái DRAFT. "
                    + "Cần kiểm tra và hoàn tất đơn trước khi "
                    + "gửi chính thức cho nhà cung cấp.";
        }


        // =====================================================
        // WAITING SUPPLIER RESPONSE
        // =====================================================

        if (
                "WAITING_SUPPLIER_RESPONSE".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp chưa phản hồi đơn mua hàng. "
                    + "Cần theo dõi hoặc liên hệ nhà cung cấp "
                    + "để xác nhận khả năng cung ứng.";
        }


        // =====================================================
        // SUPPLIER DELAYED
        // =====================================================

        if (
                "SUPPLIER_DELAYED".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp thông báo giao hàng chậm. "
                    + "Cần theo dõi ngày giao mới và đánh giá "
                    + "ảnh hưởng đến kế hoạch tồn kho.";
        }


        // =====================================================
        // SUPPLIER DECLINED
        // =====================================================

        if (
                "SUPPLIER_DECLINED".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp đã từ chối đơn hàng. "
                    + "Cần xem xét nhà cung cấp thay thế.";
        }


        // =====================================================
        // OUT OF STOCK
        // =====================================================

        if (
                "OUT_OF_STOCK".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp hiện hết hàng. "
                    + "Cần xem xét nguồn cung thay thế hoặc "
                    + "điều chỉnh kế hoạch mua hàng.";
        }


        // =====================================================
        // DISCONTINUED
        // =====================================================

        if (
                "DISCONTINUED".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp thông báo sản phẩm đã ngừng "
                    + "cung cấp. Cần tìm sản phẩm hoặc nhà cung "
                    + "cấp thay thế.";
        }


        // =====================================================
        // PRICE CHANGED
        // =====================================================

        if (
                "PRICE_CHANGED".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp đã thay đổi giá. "
                    + "Cần kiểm tra giá xác nhận trước khi "
                    + "tiếp tục xử lý đơn.";
        }


        // =====================================================
        // PARTIALLY AVAILABLE
        // =====================================================

        if (
                "PARTIALLY_AVAILABLE".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp chỉ có thể đáp ứng một phần "
                    + "số lượng đặt. Cần đánh giá phần thiếu và "
                    + "xem xét nguồn cung bổ sung.";
        }


        // =====================================================
        // PARTIALLY RECEIVED
        // =====================================================

        if (
                totalReceivedQuantity > 0L
                        && totalReceivedQuantity
                        < totalOrderedQuantity
        ) {

            return "Đơn mua hàng đã nhận một phần: "
                    + totalReceivedQuantity
                    + "/"
                    + totalOrderedQuantity
                    + " đơn vị. Cần tiếp tục theo dõi phần "
                    + "hàng còn lại.";
        }


        // =====================================================
        // SUPPLIER AVAILABLE
        // =====================================================

        if (
                "SUPPLIER_AVAILABLE".equals(
                        supplierAnalysis
                )
        ) {

            return "Nhà cung cấp đã xác nhận khả năng cung ứng. "
                    + "Cần tiếp tục theo dõi tiến độ giao và "
                    + "nhận hàng theo ngày dự kiến.";
        }


        // =====================================================
        // DEFAULT
        // =====================================================

        return "Tiếp tục theo dõi trạng thái Purchase Order, "
                + "phản hồi nhà cung cấp và tiến độ nhận hàng.";
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
    // SAFE INTEGER -> LONG
    // =========================================================

    private long safeLong(
            Integer value
    ) {

        return value == null
                ? 0L
                : value.longValue();
    }


    // =========================================================
    // SAFE BIG DECIMAL
    // =========================================================

    private BigDecimal safeBigDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}