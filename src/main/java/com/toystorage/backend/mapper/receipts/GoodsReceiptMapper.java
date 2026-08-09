package com.toystorage.backend.mapper.receipts;

import com.toystorage.backend.dto.response.receipts.GoodsReceiptResponse;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import org.springframework.stereotype.Component;

@Component
public class GoodsReceiptMapper {

    public GoodsReceiptResponse toResponse(GoodsReceipts entity) {

        if (entity == null) {
            return null;
        }

        return GoodsReceiptResponse.builder()
                .id(entity.getId())
                .goodsReceiptsCode(entity.getGoodsReceiptsCode())
                .receiptCode(entity.getReceiptCode())

                .purchaseOrderId(
                        entity.getPurchaseOrder() != null
                                ? entity.getPurchaseOrder().getId()
                                : null
                )

                .warehouseId(
                        entity.getWarehouse() != null
                                ? entity.getWarehouse().getId()
                                : null
                )

                .status(
                        entity.getStatus() != null
                                ? entity.getStatus().name()
                                : null
                )

                .confirmedBy(
                        entity.getConfirmedBy() != null
                                ? entity.getConfirmedBy().getId()
                                : null
                )

                .receivedBy(
                        entity.getReceivedBy() != null
                                ? entity.getReceivedBy().getId()
                                : null
                )

                .receivedAt(entity.getReceivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}