package com.toystorage.backend.mapper.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceiptInspectionResponse;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import org.springframework.stereotype.Component;

@Component
public class ReceiptInspectionMapper {

    public ReceiptInspectionResponse toResponse(
            ReceiptInspections entity
    ) {

        if (entity == null) {
            return null;
        }

        return ReceiptInspectionResponse.builder()
                .id(entity.getId())

                .receiptInspectionsCode(
                        entity.getReceiptInspectionsCode()
                )

                .goodsReceiptId(
                        entity.getGoodsReceipt() != null
                                ? entity.getGoodsReceipt().getId()
                                : null
                )

                .productId(
                        entity.getProduct() != null
                                ? entity.getProduct().getId()
                                : null
                )

                .packageCode(entity.getPackageCode())
                .expectedQuantity(entity.getExpectedQuantity())
                .actualQuantity(entity.getActualQuantity())

                .inspectedResult(
                        entity.getInspectedResult() != null
                                ? entity.getInspectedResult().name()
                                : null
                )

                .notes(entity.getNotes())

                .inspectedBy(
                        entity.getInspectedBy() != null
                                ? entity.getInspectedBy().getId()
                                : null
                )

                .inspectedAt(entity.getInspectedAt())
                .build();
    }
}