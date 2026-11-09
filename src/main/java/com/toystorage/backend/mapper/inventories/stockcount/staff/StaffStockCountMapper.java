package com.toystorage.backend.mapper.inventories.stockcount.staff;

import com.toystorage.backend.dto.response.inventories.stockcount.StaffStockCountDetailResponse;
import com.toystorage.backend.dto.response.inventories.stockcount.StaffStockCountItemResponse;
import com.toystorage.backend.dto.response.inventories.stockcount.StaffStockCountListResponse;

import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import com.toystorage.backend.enums.inventories.StockCountStatus;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffStockCountMapper {


    // =====================================================
    // ITEM
    // =====================================================

    public StaffStockCountItemResponse toItemResponse(
            StockCountItems item
    ) {

        return StaffStockCountItemResponse
                .builder()

                .itemId(
                        item.getId()
                )

                .productId(
                        item.getProduct().getId()
                )

                .productCode(
                        item.getProduct().getProductsCode()
                )

                .productName(
                        item.getProduct().getName()
                )

                .barcode(
                        item.getProduct().getBarcode()
                )

                .locationId(
                        item.getLocation().getId()
                )

                .locationCode(
                        item.getLocation().getWarehouseCode()
                )

                .zone(
                        item.getLocation().getZone()
                )

                .shelf(
                        item.getLocation().getShelf()
                )

                .systemQuantity(
                        item.getSystemQuantity()
                )

                .firstCountQuantity(
                        item.getFirstCountQuantity()
                )

                .secondCountQuantity(
                        item.getSecondCountQuantity()
                )

                .finalQuantity(
                        item.getFinalQuantity()
                )

                .differenceQuantity(
                        item.getDifferenceQuantity()
                )

                .countedByName(
                        item.getCountedBy() != null
                                ? item.getCountedBy().getName()
                                : null
                )

                .recountedByName(
                        item.getRecountedBy() != null
                                ? item.getRecountedBy().getName()
                                : null
                )

                .build();
    }


    // =====================================================
    // LIST
    // =====================================================

    public StaffStockCountListResponse toListResponse(
            StockCounts stockCount,
            List<StockCountItems> items
    ) {

        int counted =
                countCompletedItems(
                        stockCount,
                        items
                );


        int total =
                items.size();


        return StaffStockCountListResponse
                .builder()

                .stockCountId(
                        stockCount.getId()
                )

                .countCode(
                        stockCount.getCountCode()
                )

                .countType(
                        stockCount.getCountType() != null
                                ? stockCount
                                .getCountType()
                                .name()
                                : null
                )

                .scheduledDate(
                        stockCount.getScheduledDate()
                )

                .status(
                        stockCount
                                .getStatus()
                                .name()
                )

                .warehouseId(
                        stockCount
                                .getWarehouse()
                                .getId()
                )

                .warehouseName(
                        stockCount
                                .getWarehouse()
                                .getName()
                )

                .assignedToId(
                        stockCount.getAssignedTo() != null
                                ? stockCount
                                .getAssignedTo()
                                .getId()
                                : null
                )

                .assignedToName(
                        stockCount.getAssignedTo() != null
                                ? stockCount
                                .getAssignedTo()
                                .getName()
                                : null
                )

                .totalItems(
                        total
                )

                .countedItems(
                        counted
                )

                .remainingItems(
                        Math.max(
                                total - counted,
                                0
                        )
                )

                .available(
                        stockCount.getAssignedTo() == null
                )

                .build();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    public StaffStockCountDetailResponse toDetailResponse(
            StockCounts stockCount,
            List<StockCountItems> items
    ) {

        int counted =
                countCompletedItems(
                        stockCount,
                        items
                );


        int total =
                items.size();


        return StaffStockCountDetailResponse
                .builder()

                .stockCountId(
                        stockCount.getId()
                )

                .countCode(
                        stockCount.getCountCode()
                )

                .countType(
                        stockCount.getCountType() != null
                                ? stockCount
                                .getCountType()
                                .name()
                                : null
                )

                .scheduledDate(
                        stockCount.getScheduledDate()
                )

                .status(
                        stockCount
                                .getStatus()
                                .name()
                )

                .warehouseId(
                        stockCount
                                .getWarehouse()
                                .getId()
                )

                .warehouseName(
                        stockCount
                                .getWarehouse()
                                .getName()
                )

                .assignedToId(
                        stockCount.getAssignedTo() != null
                                ? stockCount
                                .getAssignedTo()
                                .getId()
                                : null
                )

                .assignedToName(
                        stockCount.getAssignedTo() != null
                                ? stockCount
                                .getAssignedTo()
                                .getName()
                                : null
                )

                .startedAt(
                        stockCount.getStartedAt()
                )

                .completedAt(
                        stockCount.getCompletedAt()
                )

                .totalItems(
                        total
                )

                .countedItems(
                        counted
                )

                .remainingItems(
                        Math.max(
                                total - counted,
                                0
                        )
                )

                .recount(
                        stockCount.getStatus()
                                == StockCountStatus.RECOUNTING
                )

                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }


    private int countCompletedItems(
            StockCounts stockCount,
            List<StockCountItems> items
    ) {

        if (stockCount.getStatus()
                == StockCountStatus.RECOUNTING) {

            return (int) items.stream()
                    .filter(item ->
                            item.getSecondCountQuantity()
                                    != null
                    )
                    .count();
        }


        return (int) items.stream()
                .filter(item ->
                        item.getFirstCountQuantity()
                                != null
                )
                .count();
    }
}