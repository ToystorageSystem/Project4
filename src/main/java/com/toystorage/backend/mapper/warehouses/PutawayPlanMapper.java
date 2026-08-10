package com.toystorage.backend.mapper.warehouses;

import com.toystorage.backend.dto.response.warehouses.PutawayPlanItemResponse;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import org.springframework.stereotype.Component;

@Component
public class PutawayPlanMapper {

    public PutawayPlanItemResponse toItemResponse(
            PutawayTaskItems item
    ) {

        return PutawayPlanItemResponse.builder()

                .id(item.getId())

                .productId(
                        item.getProduct().getId()
                )

                .productName(
                        item.getProduct().getName()
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
                        item.getFromLocation().getId()
                )

                .fromLocationName(
                        item.getFromLocation().getName()
                )

                .toLocationId(
                        item.getToLocation().getId()
                )

                .toLocationName(
                        item.getToLocation().getName()
                )

                .zone(
                        item.getToLocation().getZone()
                )

                .shelf(
                        item.getToLocation().getShelf()
                )

                .build();
    }
}