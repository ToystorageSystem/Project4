package com.toystorage.backend.mapper.warehouses.putaway;

import com.toystorage.backend.dto.response.warehouses.putaway.PutawayPlanItemResponse;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import org.springframework.stereotype.Component;

@Component
public class PutawayPlanMapper {

    public PutawayPlanItemResponse toItemResponse(
            PutawayTaskItems item
    ) {

        WarehouseLocations fromLocation =
                item.getFromLocation();

        WarehouseLocations toLocation =
                item.getToLocation();

        return PutawayPlanItemResponse.builder()

                .id(
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

                .expectedQuantity(
                        item.getExpectedQuantity()
                )

                .putawayQuantity(
                        item.getPutawayQuantity()
                )

                .status(
                        item.getStatus().name()
                )

                .fromLocationId(
                        fromLocation != null
                                ? fromLocation.getId()
                                : null
                )

                .fromLocationCode(
                        fromLocation != null
                                ? fromLocation.getWarehouseCode()
                                : null
                )

                .fromLocationName(
                        fromLocation != null
                                ? fromLocation.getName()
                                : null
                )

                .toLocationId(
                        toLocation != null
                                ? toLocation.getId()
                                : null
                )

                .toLocationCode(
                        toLocation != null
                                ? toLocation.getWarehouseCode()
                                : null
                )

                .toLocationName(
                        toLocation != null
                                ? toLocation.getName()
                                : null
                )

                .zone(
                        toLocation != null
                                ? toLocation.getZone()
                                : null
                )

                .shelf(
                        toLocation != null
                                ? toLocation.getShelf()
                                : null
                )

                .build();
    }
}