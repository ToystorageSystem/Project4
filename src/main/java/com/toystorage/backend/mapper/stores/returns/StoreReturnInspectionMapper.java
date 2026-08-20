package com.toystorage.backend.mapper.stores.returns;

import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionItemResponse;
import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionResponse;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoreReturnInspectionMapper {

    public StoreReturnInspectionItemResponse toItemResponse(
            StoreReturnItems item
    ) {

        return StoreReturnInspectionItemResponse
                .builder()

                .itemId(
                        item.getId()
                )

                .productId(
                        item.getProduct().getId()
                )

                .productName(
                        item.getProduct().getName()
                )

                .requestedQuantity(
                        item.getRequestedQuantity()
                )

                .issuedQuantity(
                        item.getIssuedQuantity()
                )

                .receivedQuantity(
                        item.getReceivedQuantity()
                )

                .approvedQuantity(
                        item.getApprovedQuantity()
                )

                .rejectedQuantity(
                        item.getRejectedQuantity()
                )

                .conditionStatus(
                        item.getConditionStatus() != null
                                ? item.getConditionStatus().name()
                                : null
                )

                .note(
                        item.getNote()
                )

                .fromLocationId(
                        item.getFromLocation() != null
                                ? item.getFromLocation().getId()
                                : null
                )

                .fromLocationName(
                        item.getFromLocation() != null
                                ? item.getFromLocation().getName()
                                : null
                )

                .build();
    }


    public StoreReturnInspectionResponse toResponse(
            StoreReturns storeReturn,
            List<StoreReturnItems> items
    ) {

        int requested =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getRequestedQuantity
                        )
                        .sum();

        int received =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getReceivedQuantity
                        )
                        .sum();

        int approved =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getApprovedQuantity
                        )
                        .sum();

        int rejected =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getRejectedQuantity
                        )
                        .sum();

        return StoreReturnInspectionResponse
                .builder()

                .returnId(
                        storeReturn.getId()
                )

                .returnCode(
                        storeReturn.getReturnCode()
                )

                .returnType(
                        storeReturn.getReturnType().name()
                )

                .status(
                        storeReturn.getStatus().name()
                )

                .storeId(
                        storeReturn.getStore().getId()
                )

                .storeName(
                        storeReturn.getStore().getName()
                )

                .warehouseId(
                        storeReturn.getWarehouse().getId()
                )

                .warehouseName(
                        storeReturn.getWarehouse().getName()
                )

                .totalRequestedQuantity(
                        requested
                )

                .totalReceivedQuantity(
                        received
                )

                .totalApprovedQuantity(
                        approved
                )

                .totalRejectedQuantity(
                        rejected
                )

                .receivedAt(
                        storeReturn.getReceivedAt()
                )

                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}