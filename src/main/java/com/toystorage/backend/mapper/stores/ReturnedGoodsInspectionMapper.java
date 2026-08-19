package com.toystorage.backend.mapper.stores;

import com.toystorage.backend.dto.response.stores.ReturnedGoodsDetailResponse;
import com.toystorage.backend.dto.response.stores.ReturnedGoodsItemResponse;
import com.toystorage.backend.dto.response.stores.ReturnedGoodsListResponse;

import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReturnedGoodsInspectionMapper {

    public ReturnedGoodsItemResponse toItemResponse(
            StoreReturnItems item
    ) {

        int expected =
                item.getApprovedQuantity();

        int received =
                item.getReceivedQuantity();


        return ReturnedGoodsItemResponse
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

                .expectedQuantity(
                        expected
                )

                .receivedQuantity(
                        received
                )

                .differenceQuantity(
                        received - expected
                )

                .conditionStatus(
                        item.getConditionStatus() != null
                                ? item.getConditionStatus().name()
                                : null
                )

                .note(
                        item.getNote()
                )

                .evidenceImageUrl(
                        item.getEvidenceImageUrl()
                )

                .build();
    }


    public ReturnedGoodsListResponse toListResponse(
            StoreReturns storeReturn,
            List<StoreReturnItems> items
    ) {

        int expected =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getApprovedQuantity
                        )
                        .sum();


        int received =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getReceivedQuantity
                        )
                        .sum();


        return ReturnedGoodsListResponse
                .builder()

                .returnId(
                        storeReturn.getId()
                )

                .returnCode(
                        storeReturn.getReturnCode()
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

                .totalExpectedQuantity(
                        expected
                )

                .totalReceivedQuantity(
                        received
                )

                .build();
    }


    public ReturnedGoodsDetailResponse toDetailResponse(
            StoreReturns storeReturn,
            List<StoreReturnItems> items
    ) {

        int expected =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getApprovedQuantity
                        )
                        .sum();


        int received =
                items.stream()
                        .mapToInt(
                                StoreReturnItems::getReceivedQuantity
                        )
                        .sum();


        return ReturnedGoodsDetailResponse
                .builder()

                .returnId(
                        storeReturn.getId()
                )

                .returnCode(
                        storeReturn.getReturnCode()
                )

                .status(
                        storeReturn.getStatus().name()
                )

                .returnType(
                        storeReturn.getReturnType() != null
                                ? storeReturn
                                .getReturnType()
                                .name()
                                : null
                )

                .reason(
                        storeReturn.getReason()
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

                .totalExpectedQuantity(
                        expected
                )

                .totalReceivedQuantity(
                        received
                )

                .totalDifferenceQuantity(
                        received - expected
                )

                .inspectedByName(
                        storeReturn.getInspectedBy() != null
                                ? storeReturn
                                .getInspectedBy()
                                .getName()
                                : null
                )

                .inspectionSubmittedAt(
                        storeReturn.getInspectionSubmittedAt()
                )

                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}