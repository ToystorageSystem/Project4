package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.InventoryTransferRecommendationResponse;

import com.toystorage.backend.entity.inventory.InventoryBalances;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.enums.warehouses.WarehouseType;

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
public class InventoryTransferRecommendationService {

    private final InventoryAnalyticsRepository
            inventoryAnalyticsRepository;

    private final InventoryMovementAnalyticsRepository
            inventoryMovementAnalyticsRepository;


    // =========================================================
    // INVENTORY TRANSFER RECOMMENDATION
    //
    // Logic:
    //
    // MAIN_WAREHOUSE
    // has high available inventory
    // có tồn khả dụng cao
    //
    //              ↓
    //
    // STORE
    // has lower available inventory
    // có tồn khả dụng thấp hơn
    // and has higher outbound activity than the source
    // và có outbound cao hơn source
    //
    // Only provide a recommendation.
    // Chỉ đưa recommendation.
    // DO NOT automatically create a Stock Transfer.
    // KHÔNG tự tạo Stock Transfer.
    // DO NOT automatically change inventory.
    // KHÔNG tự thay đổi tồn kho.
    // =========================================================

    public List<InventoryTransferRecommendationResponse>
    getTransferRecommendations(

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

        List<InventoryBalances> balances =
                inventoryAnalyticsRepository
                        .findAll();


        Map<String, InventoryData> inventoryMap =
                buildInventoryMap(
                        balances,
                        productId
                );


        // =====================================================
        // 2. OUTBOUND DATA
        // =====================================================

        List<Object[]> outboundRows =
                inventoryMovementAnalyticsRepository
                        .findOutboundAggregates(
                                null,
                                productId,
                                fromDate,
                                toDate
                        );


        Map<String, Long> outboundMap =
                buildOutboundMap(
                        outboundRows
                );


        // =====================================================
        // 3. GROUP INVENTORY BY PRODUCT
        // =====================================================

        Map<Long, List<InventoryData>> productInventoryMap =
                groupByProduct(
                        inventoryMap
                );


        // =====================================================
        // 4. WORKING AVAILABLE
        //
        // Used to avoid recommending the same inventory quantity multiple times
        // Dùng để tránh đề xuất cùng một lượng tồn nhiều lần
        // if there are multiple STORE locations later.
        // nếu sau này có nhiều STORE.
        // =====================================================

        Map<String, Long> workingAvailableMap =
                new HashMap<>();


        for (
                Map.Entry<String, InventoryData> entry
                : inventoryMap.entrySet()
        ) {

            workingAvailableMap.put(
                    entry.getKey(),
                    calculateAvailable(
                            entry.getValue()
                    )
            );
        }


        List<InventoryTransferRecommendationResponse> result =
                new ArrayList<>();


        // =====================================================
        // 5. ANALYZE EACH PRODUCT
        // =====================================================

        for (
                Map.Entry<Long, List<InventoryData>> productEntry
                : productInventoryMap.entrySet()
        ) {

            List<InventoryData> inventories =
                    productEntry.getValue();


            List<InventoryData> sources =
                    new ArrayList<>();


            List<InventoryData> targets =
                    new ArrayList<>();


            for (InventoryData inventory : inventories) {

                if (
                        inventory.warehouse == null
                                || inventory.warehouse.getType() == null
                ) {

                    continue;
                }


                if (
                        inventory.warehouse.getType()
                                == WarehouseType.MAIN_WAREHOUSE
                ) {

                    sources.add(
                            inventory
                    );

                } else if (
                        inventory.warehouse.getType()
                                == WarehouseType.STORE
                ) {

                    targets.add(
                            inventory
                    );
                }
            }


            // =================================================
            // Not enough source / target data
            // Không đủ source / target
            // =================================================

            if (
                    sources.isEmpty()
                            || targets.isEmpty()
            ) {

                continue;
            }


            // =================================================
            // Prioritize stores with higher outbound activity first
            // Ưu tiên store có outbound cao hơn trước
            // =================================================

            targets.sort(

                    Comparator.comparingLong(

                            (InventoryData target) ->
                                    getOutboundQuantity(
                                            target,
                                            outboundMap
                                    )

                    ).reversed()

                            .thenComparingLong(
                                    this::calculateAvailable
                            )
            );


            // =================================================
            // ANALYZE TARGET
            // =================================================

            for (InventoryData target : targets) {

                String targetKey =
                        createKey(
                                target.warehouse.getId(),
                                target.product.getId()
                        );


                long targetAvailable =
                        workingAvailableMap
                                .getOrDefault(
                                        targetKey,
                                        0L
                                );


                long targetOutbound =
                        getOutboundQuantity(
                                target,
                                outboundMap
                        );


                // =============================================
                // Select the best source:
                // Chọn source tốt nhất:
                //
                // - MAIN_WAREHOUSE
                // - available inventory is higher than the target
                // - available cao hơn target
                // - target outbound is higher than source outbound
                // - target outbound cao hơn source outbound
                //
                // If there are multiple sources:
                // Nếu nhiều source:
                // choose the source with the highest available inventory.
                // lấy source có available cao nhất.
                // =============================================

                InventoryData selectedSource =
                        null;


                long selectedSourceAvailable =
                        0L;


                long selectedSourceOutbound =
                        0L;


                for (InventoryData source : sources) {

                    String sourceKey =
                            createKey(
                                    source.warehouse.getId(),
                                    source.product.getId()
                            );


                    long sourceAvailable =
                            workingAvailableMap
                                    .getOrDefault(
                                            sourceKey,
                                            0L
                                    );


                    long sourceOutbound =
                            getOutboundQuantity(
                                    source,
                                    outboundMap
                            );


                    // =========================================
                    // The source must actually have available inventory
                    // Source phải thực sự còn tồn khả dụng
                    // =========================================

                    if (
                            sourceAvailable <= 0L
                    ) {

                        continue;
                    }


                    // =========================================
                    // The source must have more inventory than the target
                    // Source phải có tồn cao hơn target
                    // =========================================

                    if (
                            sourceAvailable
                                    <= targetAvailable
                    ) {

                        continue;
                    }


                    // =========================================
                    // The target must have higher outbound than the source
                    // Target phải có outbound cao hơn source
                    //
                    // This is a signal of actual demand.
                    // Đây là tín hiệu nhu cầu thực tế.
                    // =========================================

                    if (
                            targetOutbound
                                    <= sourceOutbound
                    ) {

                        continue;
                    }


                    // =========================================
                    // Choose the source with the highest available inventory
                    // Chọn source có available cao nhất
                    // =========================================

                    if (
                            selectedSource == null
                                    || sourceAvailable
                                    > selectedSourceAvailable
                    ) {

                        selectedSource =
                                source;


                        selectedSourceAvailable =
                                sourceAvailable;


                        selectedSourceOutbound =
                                sourceOutbound;
                    }
                }


                // =============================================
                // No suitable source was found
                // Không tìm được source phù hợp
                // =============================================

                if (
                        selectedSource == null
                ) {

                    continue;
                }


                // =============================================
                // SUGGESTED QUANTITY
                //
                // Balance available inventory:
                // Cân bằng tồn khả dụng:
                //
                // source = 125
                // target = 29
                //
                // difference = 96
                //
                // suggested = 96 / 2 = 48
                //
                // After transfer:
                // Sau chuyển:
                //
                // source = 77
                // target = 77
                //
                // Do not hard-code minStock.
                // Không hard-code minStock.
                // =============================================

                long difference =
                        selectedSourceAvailable
                                - targetAvailable;


                long suggestedQuantity =
                        difference / 2L;


                if (
                        suggestedQuantity <= 0L
                ) {

                    continue;
                }


                long sourceAvailableAfterTransfer =
                        selectedSourceAvailable
                                - suggestedQuantity;


                long targetAvailableAfterTransfer =
                        targetAvailable
                                + suggestedQuantity;


                String sourceKey =
                        createKey(
                                selectedSource.warehouse.getId(),
                                selectedSource.product.getId()
                        );


                // =============================================
                // Update working values
                //
                // Update values in memory only.
                // Chỉ update trong bộ nhớ.
                // DO NOT write to the database.
                // KHÔNG ghi database.
                // =============================================

                workingAvailableMap.put(
                        sourceKey,
                        sourceAvailableAfterTransfer
                );


                workingAvailableMap.put(
                        targetKey,
                        targetAvailableAfterTransfer
                );


                // =============================================
                // BUILD REASON
                // =============================================

                String reason =
                        buildReason(
                                selectedSourceAvailable,
                                selectedSourceOutbound,
                                targetAvailable,
                                targetOutbound,
                                suggestedQuantity
                        );


                Products product =
                        selectedSource.product;


                Warehouses sourceWarehouse =
                        selectedSource.warehouse;


                Warehouses targetWarehouse =
                        target.warehouse;


                // =============================================
                // BUILD RESPONSE
                // =============================================

                result.add(

                        InventoryTransferRecommendationResponse
                                .builder()

                                // =============================
                                // PRODUCT
                                // =============================

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


                                // =============================
                                // SOURCE
                                // =============================

                                .sourceWarehouseId(
                                        sourceWarehouse.getId()
                                )

                                .sourceWarehouseCode(
                                        sourceWarehouse
                                                .getWarehousesCode()
                                )

                                .sourceWarehouseName(
                                        sourceWarehouse.getName()
                                )

                                .sourceWarehouseType(
                                        sourceWarehouse.getType()
                                                != null
                                                ? sourceWarehouse
                                                        .getType()
                                                        .name()
                                                : null
                                )

                                .sourceAvailableQuantity(
                                        selectedSourceAvailable
                                )

                                .sourceOutboundQuantity(
                                        selectedSourceOutbound
                                )


                                // =============================
                                // TARGET
                                // =============================

                                .targetWarehouseId(
                                        targetWarehouse.getId()
                                )

                                .targetWarehouseCode(
                                        targetWarehouse
                                                .getWarehousesCode()
                                )

                                .targetWarehouseName(
                                        targetWarehouse.getName()
                                )

                                .targetWarehouseType(
                                        targetWarehouse.getType()
                                                != null
                                                ? targetWarehouse
                                                        .getType()
                                                        .name()
                                                : null
                                )

                                .targetAvailableQuantity(
                                        targetAvailable
                                )

                                .targetOutboundQuantity(
                                        targetOutbound
                                )


                                // =============================
                                // RECOMMENDATION
                                // =============================

                                .suggestedQuantity(
                                        suggestedQuantity
                                )

                                .sourceAvailableAfterTransfer(
                                        sourceAvailableAfterTransfer
                                )

                                .targetAvailableAfterTransfer(
                                        targetAvailableAfterTransfer
                                )

                                .recommendationType(
                                        "REDISTRIBUTE_TO_ACTIVE_STORE"
                                )

                                .reason(
                                        reason
                                )

                                .build()
                );
            }
        }


        // =====================================================
        // 6. SORT RESULT
        //
        // Priority:
        // Ưu tiên:
        //
        // - target outbound cao
        // - suggested quantity cao
        // =====================================================

        result.sort(

                Comparator

                        .comparing(
                                InventoryTransferRecommendationResponse
                                        ::getTargetOutboundQuantity,

                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                InventoryTransferRecommendationResponse
                                        ::getSuggestedQuantity,

                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )

                        .thenComparing(
                                InventoryTransferRecommendationResponse
                                        ::getProductName,

                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
        );


        return result;
    }


    // =========================================================
    // BUILD INVENTORY MAP
    // =========================================================

    private Map<String, InventoryData> buildInventoryMap(

            List<InventoryBalances> balances,

            Long productId
    ) {

        Map<String, InventoryData> result =
                new HashMap<>();


        if (
                balances == null
        ) {

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


            InventoryData current =
                    result.get(
                            key
                    );


            if (
                    current == null
            ) {

                current =
                        new InventoryData(
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


            current.quantity +=
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

    private Map<String, Long> buildOutboundMap(
            List<Object[]> rows
    ) {

        Map<String, Long> result =
                new HashMap<>();


        if (
                rows == null
        ) {

            return result;
        }


        for (Object[] row : rows) {

            if (
                    row == null
                            || row.length < 3
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


            String key =
                    createKey(
                            warehouseId,
                            productId
                    );


            result.put(
                    key,
                    outboundQuantity
            );
        }


        return result;
    }


    // =========================================================
    // GROUP BY PRODUCT
    // =========================================================

    private Map<Long, List<InventoryData>> groupByProduct(
            Map<String, InventoryData> inventoryMap
    ) {

        Map<Long, List<InventoryData>> result =
                new HashMap<>();


        for (
                InventoryData inventory
                : inventoryMap.values()
        ) {

            if (
                    inventory.product == null
                            || inventory.product.getId() == null
            ) {

                continue;
            }


            result.computeIfAbsent(
                    inventory.product.getId(),
                    ignored -> new ArrayList<>()
            ).add(
                    inventory
            );
        }


        return result;
    }


    // =========================================================
    // OUTBOUND QUANTITY
    // =========================================================

    private long getOutboundQuantity(

            InventoryData inventory,

            Map<String, Long> outboundMap
    ) {

        String key =
                createKey(
                        inventory.warehouse.getId(),
                        inventory.product.getId()
                );


        return outboundMap
                .getOrDefault(
                        key,
                        0L
                );
    }


    // =========================================================
    // AVAILABLE
    // =========================================================

    private long calculateAvailable(
            InventoryData inventory
    ) {

        return inventory.quantity
                - inventory.reservedQuantity;
    }


    // =========================================================
    // REASON
    // =========================================================

    private String buildReason(

            long sourceAvailable,

            long sourceOutbound,

            long targetAvailable,

            long targetOutbound,

            long suggestedQuantity
    ) {

        // Build a clear English explanation for the recommendation.
        // Tạo giải thích bằng tiếng Anh cho đề xuất điều chuyển.
        return "The source warehouse has "
                + sourceAvailable
                + " available units and "
                + sourceOutbound
                + " outbound units, while the destination store has "
                + targetAvailable
                + " available units and "
                + targetOutbound
                + " outbound units. The system recommends transferring "
                + suggestedQuantity
                + " units to balance inventory based on the current data.";
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
    // INVENTORY DATA
    // =========================================================

    private static class InventoryData {

        private final Warehouses warehouse;

        private final Products product;

        private long quantity;

        private long reservedQuantity;


        private InventoryData(

                Warehouses warehouse,

                Products product,

                long quantity,

                long reservedQuantity
        ) {

            this.warehouse =
                    warehouse;

            this.product =
                    product;

            this.quantity =
                    quantity;

            this.reservedQuantity =
                    reservedQuantity;
        }
    }
}
