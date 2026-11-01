package com.toystorage.backend.services.analytics;

import com.toystorage.backend.dto.response.analytics.InventoryAnomalyResponse;

import com.toystorage.backend.entity.inventory.InventoryBalances;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.repository.analytics.InventoryAnalyticsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAnomalyAnalyticsService {

    private final InventoryAnalyticsRepository
            inventoryAnalyticsRepository;


    // =========================================================
    // INVENTORY ANOMALY ANALYTICS
    //
    // Phát hiện bất thường trực tiếp trên từng
    // inventory balance / location.
    //
    // Các loại anomaly:
    //
    // 1. NEGATIVE_QUANTITY
    // 2. NEGATIVE_RESERVED_QUANTITY
    // 3. RESERVED_EXCEEDS_QUANTITY
    // 4. AVAILABLE_QUANTITY_MISMATCH
    //
    // Không tự sửa dữ liệu.
    // =========================================================

    public List<InventoryAnomalyResponse>
    getInventoryAnomalies(

            Long warehouseId,

            Long productId
    ) {

        // =====================================================
        // 1. LOAD INVENTORY
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


        List<InventoryAnomalyResponse> result =
                new ArrayList<>();


        // =====================================================
        // 2. ANALYZE EACH INVENTORY BALANCE
        // =====================================================

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


            long currentQuantity =
                    safeLong(
                            balance.getQuantity()
                    );


            long reservedQuantity =
                    safeLong(
                            balance.getReservedQuantity()
                    );


            long storedAvailableQuantity =
                    safeLong(
                            balance.getAvailableQuantity()
                    );


            long calculatedAvailableQuantity =
                    currentQuantity
                            - reservedQuantity;


            // =================================================
            // ANOMALY 1
            // NEGATIVE QUANTITY
            // =================================================

            if (
                    currentQuantity < 0L
            ) {

                result.add(

                        buildResponse(
                                balance,
                                warehouse,
                                product,
                                currentQuantity,
                                reservedQuantity,
                                calculatedAvailableQuantity,
                                storedAvailableQuantity,

                                "NEGATIVE_QUANTITY",

                                "CRITICAL",

                                "Số lượng tồn kho hiện tại đang âm.",

                                "Cần kiểm tra lịch sử giao dịch tồn kho "
                                        + "và đối chiếu các nghiệp vụ nhập, "
                                        + "xuất, điều chuyển hoặc điều chỉnh tồn kho."
                        )
                );
            }


            // =================================================
            // ANOMALY 2
            // NEGATIVE RESERVED QUANTITY
            // =================================================

            if (
                    reservedQuantity < 0L
            ) {

                result.add(

                        buildResponse(
                                balance,
                                warehouse,
                                product,
                                currentQuantity,
                                reservedQuantity,
                                calculatedAvailableQuantity,
                                storedAvailableQuantity,

                                "NEGATIVE_RESERVED_QUANTITY",

                                "HIGH",

                                "Số lượng hàng đang được giữ chỗ "
                                        + "không được phép nhỏ hơn 0.",

                                "Cần kiểm tra nghiệp vụ reserve, release "
                                        + "và các transaction liên quan đến "
                                        + "việc giữ hàng."
                        )
                );
            }


            // =================================================
            // ANOMALY 3
            // RESERVED EXCEEDS QUANTITY
            // =================================================

            if (
                    reservedQuantity > currentQuantity
            ) {

                result.add(

                        buildResponse(
                                balance,
                                warehouse,
                                product,
                                currentQuantity,
                                reservedQuantity,
                                calculatedAvailableQuantity,
                                storedAvailableQuantity,

                                "RESERVED_EXCEEDS_QUANTITY",

                                "HIGH",

                                "Số lượng giữ chỗ lớn hơn tổng số lượng "
                                        + "tồn kho hiện tại.",

                                "Cần kiểm tra các nghiệp vụ giữ hàng "
                                        + "vì lượng reserved đang vượt quá "
                                        + "lượng tồn thực tế."
                        )
                );
            }


            // =================================================
            // ANOMALY 4
            // AVAILABLE QUANTITY MISMATCH
            //
            // available đúng phải bằng:
            //
            // quantity - reservedQuantity
            // =================================================

            if (
                    storedAvailableQuantity
                            != calculatedAvailableQuantity
            ) {

                result.add(

                        buildResponse(
                                balance,
                                warehouse,
                                product,
                                currentQuantity,
                                reservedQuantity,
                                calculatedAvailableQuantity,
                                storedAvailableQuantity,

                                "AVAILABLE_QUANTITY_MISMATCH",

                                "MEDIUM",

                                "Tồn khả dụng đang lưu là "
                                        + storedAvailableQuantity
                                        + " nhưng giá trị tính từ quantity - "
                                        + "reservedQuantity là "
                                        + calculatedAvailableQuantity
                                        + ".",

                                "Cần đồng bộ lại availableQuantity và "
                                        + "kiểm tra luồng cập nhật inventory "
                                        + "để tránh dữ liệu tồn khả dụng bị lệch."
                        )
                );
            }
        }


        // =====================================================
        // 3. SORT
        //
        // CRITICAL
        // HIGH
        // MEDIUM
        // LOW
        //
        // Sau đó:
        // warehouse
        // product
        // inventory balance
        // =====================================================

        Comparator<InventoryAnomalyResponse> comparator =
                Comparator.comparingInt(
                        (InventoryAnomalyResponse response) ->
                                severityPriority(
                                        response.getSeverity()
                                )
                );


        comparator =
                comparator.thenComparing(
                        InventoryAnomalyResponse::getWarehouseId,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                );


        comparator =
                comparator.thenComparing(
                        InventoryAnomalyResponse::getProductName,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER
                        )
                );


        comparator =
                comparator.thenComparing(
                        InventoryAnomalyResponse::getInventoryBalanceId,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                );


        comparator =
                comparator.thenComparing(
                        InventoryAnomalyResponse::getAnomalyType,
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
    // BUILD RESPONSE
    // =========================================================

    private InventoryAnomalyResponse buildResponse(

            InventoryBalances balance,

            Warehouses warehouse,

            Products product,

            long currentQuantity,

            long reservedQuantity,

            long calculatedAvailableQuantity,

            long storedAvailableQuantity,

            String anomalyType,

            String severity,

            String description,

            String recommendation
    ) {

        Long locationId =
                balance.getLocation() != null
                        ? balance.getLocation().getId()
                        : null;


        return InventoryAnomalyResponse
                .builder()

                // =================================================
                // INVENTORY BALANCE / LOCATION
                // =================================================

                .inventoryBalanceId(
                        balance.getId()
                )

                .locationId(
                        locationId
                )


                // =================================================
                // WAREHOUSE
                // =================================================

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


                // =================================================
                // PRODUCT
                // =================================================

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


                // =================================================
                // INVENTORY
                // =================================================

                .currentQuantity(
                        currentQuantity
                )

                .reservedQuantity(
                        reservedQuantity
                )

                .calculatedAvailableQuantity(
                        calculatedAvailableQuantity
                )

                .storedAvailableQuantity(
                        storedAvailableQuantity
                )


                // =================================================
                // ANOMALY
                // =================================================

                .anomalyType(
                        anomalyType
                )

                .severity(
                        severity
                )

                .description(
                        description
                )

                .recommendation(
                        recommendation
                )

                .build();
    }


    // =========================================================
    // SEVERITY PRIORITY
    // =========================================================

    private int severityPriority(
            String severity
    ) {

        if (
                severity == null
        ) {

            return 99;
        }


        return switch (severity) {

            case "CRITICAL" -> 1;

            case "HIGH" -> 2;

            case "MEDIUM" -> 3;

            case "LOW" -> 4;

            default -> 99;
        };
    }


    // =========================================================
    // SAFE LONG
    // =========================================================

    private long safeLong(
            Integer value
    ) {

        return value == null
                ? 0L
                : value.longValue();
    }
}