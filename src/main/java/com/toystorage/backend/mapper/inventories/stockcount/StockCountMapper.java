package com.toystorage.backend.mapper.inventories.stockcount;

import com.toystorage.backend.dto.response.inventories.stockcount.StockCountItemResponse;
import com.toystorage.backend.dto.response.inventories.stockcount.StockCountResponse;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.inventories.StockCounts;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockCountMapper {

    public StockCountItemResponse toItemResponse(
            StockCountItems item
    ) {

        Users countedBy =
                item.getCountedBy();

        return StockCountItemResponse
                .builder()

                .itemId(
                        item.getId()
                )

                .productId(
                        item.getProduct().getId()
                )

                .productCode(
                        item.getProduct()
                                .getProductsCode()
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
                        item.getLocation()
                                .getWarehouseCode()
                )

                .systemQuantity(
                        item.getSystemQuantity()
                )

                .countedQuantity(
                        item.getFirstCountQuantity()
                )

                .differenceQuantity(
                        item.getDifferenceQuantity()
                )

                .result(
                        getResult(item)
                )

                .countedBy(
                        countedBy != null
                                ? countedBy.getId()
                                : null
                )

                .countedByName(
                        countedBy != null
                                ? countedBy.getName()
                                : null
                )

                .build();
    }


    public StockCountResponse toResponse(
            StockCounts stockCount,
            List<StockCountItems> items
    ) {

        Users confirmedBy =
                stockCount.getConfirmedBy();

        return StockCountResponse
                .builder()

                .stockCountId(
                        stockCount.getId()
                )

                .countCode(
                        stockCount.getCountCode()
                )

                .status(
                        stockCount.getStatus().name()
                )

                .warehouseId(
                        stockCount.getWarehouse().getId()
                )

                .warehouseName(
                        stockCount.getWarehouse().getName()
                )

                .startedAt(
                        stockCount.getStartedAt()
                )

                .confirmedBy(
                        confirmedBy != null
                                ? confirmedBy.getId()
                                : null
                )

                .confirmedByName(
                        confirmedBy != null
                                ? confirmedBy.getName()
                                : null
                )

                .confirmedAt(
                        stockCount.getConfirmedAt()
                )

                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }


    private String getResult(
            StockCountItems item
    ) {

        if (item.getFirstCountQuantity() == null) {
            return "NOT_COUNTED";
        }

        int difference =
                item.getFirstCountQuantity()
                        - item.getSystemQuantity();

        if (difference == 0) {
            return "MATCHED";
        }

        if (difference < 0) {
            return "SHORTAGE";
        }

        return "SURPLUS";
    }
}