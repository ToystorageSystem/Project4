package com.toystorage.backend.services.warehouses;

import com.toystorage.backend.dto.response.warehouses.WarehouseDeactivationImpactResponse;

import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.warehouses.WarehouseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseImpactService {

    private final WarehouseRepository warehouseRepository;

    private final JdbcTemplate jdbcTemplate;


    // =========================================================
    // CHECK DEACTIVATION IMPACT
    // =========================================================

    @Transactional(readOnly = true)
    public WarehouseDeactivationImpactResponse
    getDeactivationImpact(
            Long warehouseId
    ) {

        Warehouses warehouse =
                warehouseRepository
                        .findById(warehouseId)
                        .orElseThrow(() ->
                                new NotFound(
                                        "Không tìm thấy kho hoặc cửa hàng"
                                )
                        );


        Map<String, Long> blockingByModule =
                new LinkedHashMap<>();


        // =====================================================
        // GOODS RECEIPT
        // CLOSED:
        // COMPLETED, CANCELLED
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "GOODS_RECEIPT",
                count(
                        """
                        SELECT COUNT(*)
                        FROM goods_receipts
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // PURCHASE ORDER
        // CLOSED:
        // COMPLETED, CANCELLED
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "PURCHASE_ORDER",
                count(
                        """
                        SELECT COUNT(*)
                        FROM purchase_orders
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // STOCK TRANSFER
        // CLOSED:
        // COMPLETED, CANCELLED, REJECTED
        //
        // RECEIVED vẫn chưa tính là đóng vì enum còn COMPLETED.
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "STOCK_TRANSFER",
                count(
                        """
                        SELECT COUNT(*)
                        FROM stock_transfers
                        WHERE (
                            from_warehouse_id = ?
                            OR to_warehouse_id = ?
                        )
                        AND status NOT IN (
                            'COMPLETED',
                            'CANCELLED',
                            'REJECTED'
                        )
                        """,
                        warehouseId,
                        warehouseId
                )
        );


        // =====================================================
        // STORE RECEIPT
        // CLOSED:
        // COMPLETED, CANCELLED, REJECTED
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "STORE_RECEIPT",
                count(
                        """
                        SELECT COUNT(*)
                        FROM store_receipts
                        WHERE store_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED',
                              'REJECTED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // STORE RETURN
        // CLOSED:
        // RECEIVED, CANCELLED
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "STORE_RETURN",
                count(
                        """
                        SELECT COUNT(*)
                        FROM store_returns
                        WHERE (
                            store_id = ?
                            OR warehouse_id = ?
                        )
                        AND status NOT IN (
                            'RECEIVED',
                            'CANCELLED'
                        )
                        """,
                        warehouseId,
                        warehouseId
                )
        );


        // =====================================================
        // DELIVERY
        // CLOSED:
        // DELIVERED, CANCELLED, FAILED
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "DELIVERY",
                count(
                        """
                        SELECT COUNT(*)
                        FROM deliveries
                        WHERE (
                            from_warehouse_id = ?
                            OR to_warehouse_id = ?
                        )
                        AND status NOT IN (
                            'DELIVERED',
                            'CANCELLED',
                            'FAILED'
                        )
                        """,
                        warehouseId,
                        warehouseId
                )
        );


        // =====================================================
        // PACKAGE
        // CLOSED:
        // RECEIVED, CANCELLED
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "PACKAGE",
                count(
                        """
                        SELECT COUNT(*)
                        FROM packages
                        WHERE (
                            from_warehouse_id = ?
                            OR to_warehouse_id = ?
                        )
                        AND status NOT IN (
                            'RECEIVED',
                            'CANCELLED'
                        )
                        """,
                        warehouseId,
                        warehouseId
                )
        );


        // =====================================================
        // SHIPMENT MANIFEST
        //
        // PRINTED được xem là bước kết thúc của workflow manifest.
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "SHIPMENT_MANIFEST",
                count(
                        """
                        SELECT COUNT(*)
                        FROM shipment_manifests
                        WHERE (
                            from_warehouse_id = ?
                            OR to_warehouse_id = ?
                        )
                        AND status NOT IN (
                            'PRINTED',
                            'CANCELLED'
                        )
                        """,
                        warehouseId,
                        warehouseId
                )
        );


        // =====================================================
        // PUTAWAY
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "PUTAWAY_TASK",
                count(
                        """
                        SELECT COUNT(*)
                        FROM putaway_tasks
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // STOCK COUNT
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "STOCK_COUNT",
                count(
                        """
                        SELECT COUNT(*)
                        FROM stock_counts
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // INVENTORY ADJUSTMENT
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "INVENTORY_ADJUSTMENT",
                count(
                        """
                        SELECT COUNT(*)
                        FROM inventory_adjustments
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED',
                              'REJECTED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // REPLENISHMENT
        //
        // APPROVED / TRANSFER_CREATED vẫn được xem là đang xử lý.
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "REPLENISHMENT_REQUEST",
                count(
                        """
                        SELECT COUNT(*)
                        FROM replenishment_requests
                        WHERE store_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED',
                              'REJECTED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // SALES ORDER
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "SALES_ORDER",
                count(
                        """
                        SELECT COUNT(*)
                        FROM sales_orders
                        WHERE store_id = ?
                          AND status NOT IN (
                              'COMPLETED',
                              'CANCELLED',
                              'REFUNDED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // DISCREPANCY
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "DISCREPANCY_REPORT",
                count(
                        """
                        SELECT COUNT(*)
                        FROM discrepancy_reports
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'RESOLVED',
                              'CANCELLED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // RECEIVING INCIDENT
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "RECEIVING_INCIDENT",
                count(
                        """
                        SELECT COUNT(*)
                        FROM receiving_incident_reports
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'CONFIRMED',
                              'CANCELLED'
                          )
                        """,
                        warehouseId
                )
        );


        // =====================================================
        // DAMAGED GOODS
        //
        // APPROVED chưa mặc định là đóng:
        // report có thể còn bước xử lý/dispose sau approval.
        // =====================================================

        addIfBlocking(
                blockingByModule,
                "DAMAGED_GOODS",
                count(
                        """
                        SELECT COUNT(*)
                        FROM damaged_goods_reports
                        WHERE warehouse_id = ?
                          AND status NOT IN (
                              'DISPOSED',
                              'REJECTED'
                          )
                        """,
                        warehouseId
                )
        );


        long totalBlockingRecords =
                blockingByModule
                        .values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .sum();


        return WarehouseDeactivationImpactResponse
                .builder()
                .warehouseId(
                        warehouse.getId()
                )
                .warehousesCode(
                        warehouse.getWarehousesCode()
                )
                .warehouseName(
                        warehouse.getName()
                )
                .status(
                        warehouse.getStatus()
                )
                .canDeactivate(
                        totalBlockingRecords == 0
                )
                .totalBlockingRecords(
                        totalBlockingRecords
                )
                .blockingByModule(
                        blockingByModule
                )
                .build();
    }


    // =========================================================
    // COUNT HELPER
    // =========================================================

    private long count(
            String sql,
            Object... params
    ) {

        Long result =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class,
                        params
                );

        return result != null
                ? result
                : 0L;
    }


    // =========================================================
    // ONLY RETURN MODULES THAT REALLY BLOCK
    // =========================================================

    private void addIfBlocking(
            Map<String, Long> blockingByModule,
            String module,
            long count
    ) {

        if (count > 0) {

            blockingByModule.put(
                    module,
                    count
            );
        }
    }
}