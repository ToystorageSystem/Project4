package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.InventoryMovementResponse;

import com.toystorage.backend.entity.inventories.InventoryBalances;
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
public class InventoryMovementAnalyticsService {

    private final InventoryAnalyticsRepository
            inventoryAnalyticsRepository;

    private final InventoryMovementAnalyticsRepository
            inventoryMovementAnalyticsRepository;


    // =========================================================
    // INVENTORY MOVEMENT ANALYTICS
    // =========================================================

    public List<InventoryMovementResponse> getInventoryMovement(

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
        //
        // Load the current inventory_balances.
        // Lấy inventory_balances hiện tại.
        //
        // Do not directly use available_quantity stored in the DB.
        // Không sử dụng trực tiếp available_quantity lưu trong DB.
        //
        // available =
        //      quantity - reservedQuantity
        //
        // because the DB previously contained available_quantity data
        // vì trước đó DB từng có dữ liệu available_quantity
        // that was out of sync.
        // không đồng bộ.
        // =====================================================

        List<InventoryBalances> balances =
                inventoryAnalyticsRepository.findAll();


        Map<String, CurrentInventoryData> currentInventoryMap =
                buildCurrentInventoryMap(

                        balances,

                        warehouseId,

                        productId
                );


        // =====================================================
        // 2. INVENTORY MOVEMENTS
        // =====================================================

        List<Object[]> movementRows =
                inventoryMovementAnalyticsRepository
                        .findMovementAggregates(

                                warehouseId,

                                productId,

                                fromDate,

                                toDate
                        );


        Map<String, MovementData> movementMap =
                buildMovementMap(
                        movementRows
                );


        // =====================================================
        // 3. CALCULATE AVERAGE POSITIVE MOVEMENT
        //
        // Do not use hard-coded thresholds.
        // Không dùng ngưỡng hard-code.
        //
        // Only include warehouse-product pairs with movement > 0.
        // Chỉ tính các warehouse-product có movement > 0.
        // =====================================================

        double averagePositiveMovement =
                calculateAveragePositiveMovement(

                        currentInventoryMap,

                        movementMap
                );


        // =====================================================
        // 4. BUILD RESPONSE
        // =====================================================

        List<InventoryMovementResponse> result =
                new ArrayList<>();


        for (
                Map.Entry<String, CurrentInventoryData> entry
                : currentInventoryMap.entrySet()
        ) {

            String key =
                    entry.getKey();


            CurrentInventoryData current =
                    entry.getValue();


            MovementData movement =
                    movementMap.getOrDefault(

                            key,

                            MovementData.empty()
                    );


            long availableQuantity =
                    current.currentQuantity
                            - current.reservedQuantity;


            long netMovement =
                    movement.totalInbound
                            - movement.totalOutbound;


            long totalMovement =
                    movement.totalInbound
                            + movement.totalOutbound;


            String movementStatus =
                    classifyMovement(

                            totalMovement,

                            averagePositiveMovement
                    );


            String recommendation =
                    buildRecommendation(

                            movementStatus,

                            current.currentQuantity,

                            availableQuantity,

                            totalMovement
                    );


            Warehouses warehouse =
                    current.warehouse;


            Products product =
                    current.product;


            result.add(

                    InventoryMovementResponse
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
                            // MOVEMENT
                            // =================================

                            .totalInbound(
                                    movement.totalInbound
                            )

                            .totalOutbound(
                                    movement.totalOutbound
                            )

                            .netMovement(
                                    netMovement
                            )

                            .totalMovement(
                                    totalMovement
                            )

                            .transactionCount(
                                    movement.transactionCount
                            )

                            .lastMovementAt(
                                    movement.lastMovementAt
                            )


                            // =================================
                            // ANALYTICS
                            // =================================

                            .movementStatus(
                                    movementStatus
                            )

                            .recommendation(
                                    recommendation
                            )

                            .build()
            );
        }


        // =====================================================
        // 5. SORT
        //
        // warehouse ASC
        // totalMovement DESC
        // productName ASC
        // =====================================================

        result.sort(

                Comparator

                        .comparing(
                                InventoryMovementResponse::getWarehouseId,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )

                        .thenComparing(
                                InventoryMovementResponse::getTotalMovement,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                InventoryMovementResponse::getProductName,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
        );


        return result;
    }


    // =========================================================
    // BUILD CURRENT INVENTORY MAP
    // =========================================================

    private Map<String, CurrentInventoryData>
    buildCurrentInventoryMap(

            List<InventoryBalances> balances,

            Long warehouseId,

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


            // -------------------------------------------------
            // WAREHOUSE FILTER
            // -------------------------------------------------

            if (
                    warehouseId != null
                            && !warehouseId.equals(
                                    warehouse.getId()
                            )
            ) {

                continue;
            }


            // -------------------------------------------------
            // PRODUCT FILTER
            // -------------------------------------------------

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
    // BUILD MOVEMENT MAP
    // =========================================================

    private Map<String, MovementData> buildMovementMap(
            List<Object[]> rows
    ) {

        Map<String, MovementData> result =
                new HashMap<>();


        if (rows == null) {
            return result;
        }


        for (Object[] row : rows) {

            if (
                    row == null
                            || row.length < 6
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


            Long totalInbound =
                    toLong(
                            row[2]
                    );


            Long totalOutbound =
                    toLong(
                            row[3]
                    );


            Long transactionCount =
                    toLong(
                            row[4]
                    );


            LocalDateTime lastMovementAt =
                    row[5] instanceof LocalDateTime value
                            ? value
                            : null;


            String key =
                    createKey(

                            warehouseId,

                            productId
                    );


            result.put(

                    key,

                    new MovementData(

                            totalInbound,

                            totalOutbound,

                            transactionCount,

                            lastMovementAt
                    )
            );
        }


        return result;
    }


    // =========================================================
    // CALCULATE AVERAGE MOVEMENT
    //
    // Use only data within the current analysis scope.
    // Dùng chính dữ liệu trong phạm vi đang phân tích.
    //
    // Do not hard-code:
    // Không hard-code:
    // 10, 20, 50...
    // =========================================================

    private double calculateAveragePositiveMovement(

            Map<String, CurrentInventoryData> inventoryMap,

            Map<String, MovementData> movementMap
    ) {

        long totalMovement =
                0L;


        long movementItemCount =
                0L;


        for (String key : inventoryMap.keySet()) {

            MovementData movement =
                    movementMap.getOrDefault(

                            key,

                            MovementData.empty()
                    );


            long movementQuantity =
                    movement.totalInbound
                            + movement.totalOutbound;


            if (movementQuantity <= 0L) {
                continue;
            }


            totalMovement +=
                    movementQuantity;


            movementItemCount++;
        }


        if (movementItemCount == 0L) {

            return 0D;
        }


        return (double) totalMovement
                / movementItemCount;
    }


    // =========================================================
    // CLASSIFY MOVEMENT
    //
    // Do not use artificial business thresholds.
    // Không dùng business threshold giả lập.
    //
    // NO_MOVEMENT:
    //      không phát sinh transaction.
    //
    // LOW_MOVEMENT:
    //      movement thấp hơn trung bình của tập dữ liệu.
    //
    // NORMAL:
    //      movement bằng mức trung bình.
    //
    // HIGH_MOVEMENT:
    //      movement cao hơn trung bình.
    // =========================================================

    private String classifyMovement(

            long totalMovement,

            double averagePositiveMovement
    ) {

        if (totalMovement <= 0L) {

            return "NO_MOVEMENT";
        }


        if (averagePositiveMovement <= 0D) {

            return "NORMAL";
        }


        if (
                totalMovement
                        < averagePositiveMovement
        ) {

            return "LOW_MOVEMENT";
        }


        if (
                totalMovement
                        > averagePositiveMovement
        ) {

            return "HIGH_MOVEMENT";
        }


        return "NORMAL";
    }


    // =========================================================
    // RECOMMENDATION
    // =========================================================

    private String buildRecommendation(

            String movementStatus,

            long currentQuantity,

            long availableQuantity,

            long totalMovement
    ) {

        if (
                availableQuantity < 0L
        ) {

            // Available inventory is negative and requires verification.
            // Tồn khả dụng đang âm và cần được kiểm tra.
            return "Available inventory is negative. "
                    + "Check the reserved quantity and inventory data.";
        }


        return switch (movementStatus) {

            case "NO_MOVEMENT" ->

                    currentQuantity > 0L

                            // Stock exists but no inventory movement was recorded.
                            // Có tồn kho nhưng không phát sinh biến động tồn kho.
                            ? "The product is currently in stock but no inventory "
                            + "movement was recorded during the analyzed period. "
                            + "Continue monitoring for potential long-term inventory."

                            // No stock and no inventory movement were recorded.
                            // Không có tồn kho và không phát sinh biến động tồn kho.
                            : "The product is out of stock and no inventory "
                            + "movement was recorded during the analyzed period.";


            case "LOW_MOVEMENT" ->

                    // Inventory movement is below the average level.
                    // Biến động tồn kho thấp hơn mức trung bình.
                    "Inventory movement is below the average level within "
                            + "the analyzed data range. Monitor the inventory "
                            + "turnover rate.";


            case "HIGH_MOVEMENT" ->

                    // Inventory movement is above the average level.
                    // Biến động tồn kho cao hơn mức trung bình.
                    "The product has a higher-than-average inventory movement "
                            + "level. Monitor available inventory to reduce the "
                            + "risk of stock shortage.";


            default ->

                    // Inventory movement is at the average level.
                    // Biến động tồn kho đang ở mức trung bình.
                    "Inventory movement is at an average level within "
                            + "the analyzed data range.";
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
    // KEY
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
    // MOVEMENT DATA
    // =========================================================

    private static class MovementData {

        private final long totalInbound;

        private final long totalOutbound;

        private final long transactionCount;

        private final LocalDateTime lastMovementAt;


        private MovementData(

                Long totalInbound,

                Long totalOutbound,

                Long transactionCount,

                LocalDateTime lastMovementAt
        ) {

            this.totalInbound =
                    totalInbound == null
                            ? 0L
                            : totalInbound;


            this.totalOutbound =
                    totalOutbound == null
                            ? 0L
                            : totalOutbound;


            this.transactionCount =
                    transactionCount == null
                            ? 0L
                            : transactionCount;


            this.lastMovementAt =
                    lastMovementAt;
        }


        private static MovementData empty() {

            return new MovementData(

                    0L,

                    0L,

                    0L,

                    null
            );
        }
    }
}
