package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.SlowMovingInventoryResponse;
import com.toystorage.backend.entity.inventory.InventoryBalances;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.analytics.InventoryAnalyticsRepository;
import com.toystorage.backend.repository.analytics.InventoryMovementAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlowMovingInventoryAnalyticsService {

    private final InventoryAnalyticsRepository
            inventoryAnalyticsRepository;

    private final InventoryMovementAnalyticsRepository
            inventoryMovementAnalyticsRepository;


    // =========================================================
    // SLOW-MOVING INVENTORY ANALYTICS
    // =========================================================

    public List<SlowMovingInventoryResponse> getSlowMovingInventory(

            Long warehouseId,

            Long productId,

            LocalDateTime fromDate,

            LocalDateTime toDate
    ) {

        validateDateRange(
                fromDate,
                toDate
        );


        // =====================================================
        // 1. CURRENT INVENTORY
        // =====================================================

        List<InventoryBalances> balances;

        if (warehouseId != null) {

            balances =
                    inventoryAnalyticsRepository
                            .findByWarehouse_Id(
                                    warehouseId
                            );

        } else {

            balances =
                    inventoryAnalyticsRepository
                            .findAll();
        }


        Map<String, CurrentInventoryData> inventoryMap =
                buildCurrentInventoryMap(
                        balances,
                        productId
                );


        // =====================================================
        // 2. OUTBOUND MOVEMENT
        // =====================================================

        List<Object[]> outboundRows =
                inventoryMovementAnalyticsRepository
                        .findOutboundAggregates(
                                warehouseId,
                                productId,
                                fromDate,
                                toDate
                        );


        Map<String, OutboundData> outboundMap =
                buildOutboundMap(
                        outboundRows
                );


        // =====================================================
        // 3. AVERAGE POSITIVE OUTBOUND
        //
        // Only include warehouse-product pairs that actually
        // Chỉ tính các warehouse-product thực sự
        // have outbound activity.
        // có phát sinh outbound.
        //
        // Do not use artificial business thresholds such as:
        // Không dùng ngưỡng business giả như:
        //
        // - 10 products
        // - 10 sản phẩm
        // - 30 days
        // - 30 ngày
        // - 60 days
        // - 60 ngày
        // =====================================================

        double averagePositiveOutbound =
                calculateAveragePositiveOutbound(
                        inventoryMap,
                        outboundMap
                );


        // =====================================================
        // 4. BUILD RESPONSE
        // =====================================================

        List<SlowMovingInventoryResponse> result =
                new ArrayList<>();


        for (
                Map.Entry<String, CurrentInventoryData> entry
                : inventoryMap.entrySet()
        ) {

            String key =
                    entry.getKey();


            CurrentInventoryData current =
                    entry.getValue();


            OutboundData outbound =
                    outboundMap.getOrDefault(
                            key,
                            OutboundData.empty()
                    );


            // =================================================
            // AVAILABLE QUANTITY
            //
            // Do not use the stored availableQuantity because old data
            // Không dùng availableQuantity lưu sẵn vì dữ liệu cũ
            // may be out of sync.
            // có thể chưa đồng bộ.
            //
            // available = quantity - reserved
            // =================================================

            long availableQuantity =
                    current.currentQuantity
                            - current.reservedQuantity;


            // =================================================
            // MOVEMENT TYPE
            // =================================================

            String movementType =
                    classifyMovement(
                            outbound.outboundQuantity,
                            averagePositiveOutbound
                    );


            // =================================================
            // RECOMMENDATION
            // =================================================

            String recommendation =
                    buildRecommendation(
                            movementType,
                            current.currentQuantity,
                            availableQuantity
                    );


            Warehouses warehouse =
                    current.warehouse;


            Products product =
                    current.product;


            SlowMovingInventoryResponse response =
                    SlowMovingInventoryResponse
                            .builder()

                            // =================================
                            // WAREHOUSE
                            // =================================

                            .warehouseId(
                                    warehouse.getId()
                            )

                            .warehouseCode(
                                    warehouse.getWarehousesCode()
                            )

                            .warehouseName(
                                    warehouse.getName()
                            )

                            .warehouseType(
                                    warehouse.getType() != null
                                            ? warehouse
                                                    .getType()
                                                    .name()
                                            : null
                            )


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


                            // =================================
                            // CURRENT INVENTORY
                            // =================================

                            .currentQuantity(
                                    current.currentQuantity
                            )

                            .reservedQuantity(
                                    current.reservedQuantity
                            )

                            .availableQuantity(
                                    availableQuantity
                            )


                            // =================================
                            // OUTBOUND
                            // =================================

                            .outboundQuantity(
                                    outbound.outboundQuantity
                            )

                            .outboundTransactionCount(
                                    outbound.outboundTransactionCount
                            )

                            .lastOutboundAt(
                                    outbound.lastOutboundAt
                            )


                            // =================================
                            // ANALYTICS
                            // =================================

                            .movementType(
                                    movementType
                            )

                            .recommendation(
                                    recommendation
                            )

                            .build();


            result.add(
                    response
            );
        }


        // =====================================================
        // 5. SORT
        //
        // Priority:
        // Ưu tiên:
        //
        // 1. NO_OUTBOUND
        // 2. SLOW_MOVING
        // 3. NORMAL
        // 4. FAST_MOVING
        //
        // Then:
        // Sau đó:
        //
        // availableQuantity DESC
        // warehouseId ASC
        // productName ASC
        //
        // IMPORTANT:
        // QUAN TRỌNG:
        //
        // Explicitly declare the SlowMovingInventoryResponse type
        // Khai báo rõ kiểu SlowMovingInventoryResponse
        // in the lambda so Java 17 does not infer Object.
        // trong lambda để Java 17 không suy luận thành Object.
        // =====================================================

        Comparator<SlowMovingInventoryResponse> comparator =
                Comparator.comparingInt(
                        (SlowMovingInventoryResponse response) ->
                                movementPriority(
                                        response.getMovementType()
                                )
                );


        comparator =
                comparator.thenComparing(
                        SlowMovingInventoryResponse::getAvailableQuantity,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                );


        comparator =
                comparator.thenComparing(
                        SlowMovingInventoryResponse::getWarehouseId,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                );


        comparator =
                comparator.thenComparing(
                        SlowMovingInventoryResponse::getProductName,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER
                        )
                );


        result.sort(
                comparator
        );


        return result;
    }


    // =========================================================
    // BUILD CURRENT INVENTORY MAP
    // =========================================================

    private Map<String, CurrentInventoryData>
    buildCurrentInventoryMap(

            List<InventoryBalances> balances,

            Long productId
    ) {

        Map<String, CurrentInventoryData> result =
                new HashMap<>();


        if (balances == null) {

            return result;
        }


        for (InventoryBalances balance : balances) {

            if (
                    balance == null
                            || balance.getWarehouse() == null
                            || balance.getProduct() == null
            ) {

                continue;
            }


            Warehouses warehouse =
                    balance.getWarehouse();


            Products product =
                    balance.getProduct();


            // =================================================
            // PRODUCT FILTER
            // =================================================

            if (
                    productId != null
                            && !productId.equals(
                                    product.getId()
                            )
            ) {

                continue;
            }


            String key =
                    createKey(
                            warehouse.getId(),
                            product.getId()
                    );


            CurrentInventoryData current =
                    result.get(
                            key
                    );


            if (current == null) {

                current =
                        new CurrentInventoryData(
                                warehouse,
                                product,
                                0L,
                                0L
                        );


                result.put(
                        key,
                        current
                );
            }


            current.currentQuantity +=
                    safeLong(
                            balance.getQuantity()
                    );


            current.reservedQuantity +=
                    safeLong(
                            balance.getReservedQuantity()
                    );
        }


        return result;
    }


    // =========================================================
    // BUILD OUTBOUND MAP
    // =========================================================

    private Map<String, OutboundData> buildOutboundMap(
            List<Object[]> rows
    ) {

        Map<String, OutboundData> result =
                new HashMap<>();


        if (rows == null) {

            return result;
        }


        for (Object[] row : rows) {

            if (
                    row == null
                            || row.length < 5
                            || row[0] == null
                            || row[1] == null
            ) {

                continue;
            }


            Long warehouseId =
                    toLong(
                            row[0]
                    );


            Long productId =
                    toLong(
                            row[1]
                    );


            Long outboundQuantity =
                    toLong(
                            row[2]
                    );


            Long outboundTransactionCount =
                    toLong(
                            row[3]
                    );


            LocalDateTime lastOutboundAt =
                    row[4] instanceof LocalDateTime value
                            ? value
                            : null;


            String key =
                    createKey(
                            warehouseId,
                            productId
                    );


            OutboundData outboundData =
                    new OutboundData(
                            outboundQuantity,
                            outboundTransactionCount,
                            lastOutboundAt
                    );


            result.put(
                    key,
                    outboundData
            );
        }


        return result;
    }


    // =========================================================
    // AVERAGE POSITIVE OUTBOUND
    //
    // Example:
    // Ví dụ:
    //
    // Product A outbound = 20
    // Product B outbound = 10
    // Product C outbound = 0
    //
    // Average positive outbound:
    //
    // (20 + 10) / 2
    //
    // Products with outbound = 0 are excluded from the average,
    // Product có outbound = 0 không tham gia vào average,
    // because they are classified separately as NO_OUTBOUND.
    // vì nó đã được phân loại riêng là NO_OUTBOUND.
    // =========================================================

    private double calculateAveragePositiveOutbound(

            Map<String, CurrentInventoryData> inventoryMap,

            Map<String, OutboundData> outboundMap
    ) {

        long totalOutbound =
                0L;


        long productCount =
                0L;


        for (String key : inventoryMap.keySet()) {

            OutboundData outbound =
                    outboundMap.getOrDefault(
                            key,
                            OutboundData.empty()
                    );


            if (
                    outbound.outboundQuantity <= 0L
            ) {

                continue;
            }


            totalOutbound +=
                    outbound.outboundQuantity;


            productCount++;
        }


        if (productCount == 0L) {

            return 0D;
        }


        return (double) totalOutbound
                / productCount;
    }


    // =========================================================
    // CLASSIFY MOVEMENT
    //
    // NO_OUTBOUND
    //
    // No inventory-decreasing transactions.
    // Không có giao dịch giảm tồn.
    //
    // SLOW_MOVING
    //
    // Outbound is below the average.
    // Outbound thấp hơn average.
    //
    // NORMAL
    //
    // Outbound equals the average.
    // Outbound bằng average.
    //
    // FAST_MOVING
    //
    // Outbound is above the average.
    // Outbound cao hơn average.
    // =========================================================

    private String classifyMovement(

            long outboundQuantity,

            double averagePositiveOutbound
    ) {

        if (
                outboundQuantity <= 0L
        ) {

            return "NO_OUTBOUND";
        }


        if (
                averagePositiveOutbound <= 0D
        ) {

            return "NORMAL";
        }


        if (
                outboundQuantity
                        < averagePositiveOutbound
        ) {

            return "SLOW_MOVING";
        }


        if (
                outboundQuantity
                        > averagePositiveOutbound
        ) {

            return "FAST_MOVING";
        }


        return "NORMAL";
    }


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    private String buildRecommendation(

            String movementType,

            long currentQuantity,

            long availableQuantity
    ) {

        // =====================================================
        // DATA ABNORMAL
        // =====================================================

        if (
                availableQuantity < 0L
        ) {

            // Available inventory is negative and requires verification.
            // Tồn khả dụng đang âm và cần được kiểm tra.
            return "Available inventory is negative. "
                    + "Check the inventory data and reserved quantity.";
        }


        // =====================================================
        // MOVEMENT RECOMMENDATION
        // =====================================================

        return switch (movementType) {

            case "NO_OUTBOUND" -> {

                if (
                        currentQuantity > 0L
                ) {

                    // Stock exists but no outbound activity was recorded.
                    // Còn tồn kho nhưng không phát sinh giao dịch làm giảm tồn.
                    yield "The product is currently in stock but no outbound "
                            + "inventory activity was recorded during the analyzed "
                            + "period. This may indicate a risk of long-term inventory.";
                }


                // No stock and no outbound activity were recorded.
                // Không còn tồn kho và không phát sinh giao dịch làm giảm tồn.
                yield "The product is out of stock and no outbound inventory "
                        + "activity was recorded during the analyzed period.";
            }


            case "SLOW_MOVING" ->

                    // Outbound quantity is lower than the average.
                    // Lượng xuất khỏi tồn kho thấp hơn mức trung bình.
                    "The outbound quantity is below the average of products "
                            + "with outbound activity in the analyzed data range. "
                            + "Monitor demand and replenishment planning.";


            case "FAST_MOVING" ->

                    // Outbound quantity is higher than the average.
                    // Lượng xuất khỏi tồn kho cao hơn mức trung bình.
                    "The product has a higher-than-average outbound quantity "
                            + "within the analyzed data range. Monitor available "
                            + "inventory to reduce the risk of stock shortage.";


            default ->

                    // Outbound activity is close to the average level.
                    // Mức xuất khỏi tồn kho tương đương mức trung bình.
                    "The product has an outbound inventory level close to "
                            + "the average of the analyzed data.";
        };
    }


    // =========================================================
    // MOVEMENT PRIORITY
    //
    // Used to sort the results.
    // Dùng để sort kết quả.
    // =========================================================

    private int movementPriority(
            String movementType
    ) {

        if (
                movementType == null
        ) {

            return 99;
        }


        return switch (movementType) {

            case "NO_OUTBOUND" -> 1;

            case "SLOW_MOVING" -> 2;

            case "NORMAL" -> 3;

            case "FAST_MOVING" -> 4;

            default -> 99;
        };
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

            // The start date must not be later than the end date.
            // Thời gian bắt đầu không được sau thời gian kết thúc.
            throw new BadRequest(
                    "The start date must not be later than the end date."
            );
        }
    }


    // =========================================================
    // CREATE MAP KEY
    // =========================================================

    private String createKey(

            Long warehouseId,

            Long productId
    ) {

        return warehouseId
                + ":"
                + productId;
    }


    // =========================================================
    // SAFE CONVERSION
    // =========================================================

    private Long toLong(
            Object value
    ) {

        if (
                value == null
        ) {

            return 0L;
        }


        if (
                value instanceof Number number
        ) {

            return number.longValue();
        }


        return Long.parseLong(
                value.toString()
        );
    }


    private long safeLong(
            Integer value
    ) {

        return value == null
                ? 0L
                : value.longValue();
    }


    // =========================================================
    // CURRENT INVENTORY DATA
    // =========================================================

    private static class CurrentInventoryData {

        private final Warehouses warehouse;

        private final Products product;

        private long currentQuantity;

        private long reservedQuantity;


        private CurrentInventoryData(

                Warehouses warehouse,

                Products product,

                long currentQuantity,

                long reservedQuantity
        ) {

            this.warehouse =
                    warehouse;


            this.product =
                    product;


            this.currentQuantity =
                    currentQuantity;


            this.reservedQuantity =
                    reservedQuantity;
        }
    }


    // =========================================================
    // OUTBOUND DATA
    // =========================================================

    private static class OutboundData {

        private final long outboundQuantity;

        private final long outboundTransactionCount;

        private final LocalDateTime lastOutboundAt;


        private OutboundData(

                Long outboundQuantity,

                Long outboundTransactionCount,

                LocalDateTime lastOutboundAt
        ) {

            this.outboundQuantity =
                    outboundQuantity == null
                            ? 0L
                            : outboundQuantity;


            this.outboundTransactionCount =
                    outboundTransactionCount == null
                            ? 0L
                            : outboundTransactionCount;


            this.lastOutboundAt =
                    lastOutboundAt;
        }


        private static OutboundData empty() {

            return new OutboundData(
                    0L,
                    0L,
                    null
            );
        }
    }
}
