package com.toystorage.backend.mapper.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingDetailResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingItemResponse;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceivingInspectionMapper {

    public ReceivingItemResponse toItemResponse(
            GoodsReceiptItems item,
            ReceiptInspections inspection
    ) {

        int difference =
                item.getActualQuantity()
                        - item.getExpectedQuantity();

        return ReceivingItemResponse.builder()

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

                .expectedQuantity(
                        item.getExpectedQuantity()
                )

                .actualQuantity(
                        item.getActualQuantity()
                )

                .acceptedQuantity(
                        item.getAcceptedQuantity()
                )

                .damagedQuantity(
                        item.getDamagedQuantity()
                )

                .shortageQuantity(
                        item.getShortageQuantity()
                )

                .surplusQuantity(
                        item.getSurplusQuantity()
                )

                .differenceQuantity(
                        difference
                )

                .inspectionResult(
                        inspection != null
                                ? inspection
                                .getInspectedResult()
                                .name()
                                : null
                )

                .packageCode(
                        inspection != null
                                ? inspection.getPackageCode()
                                : null
                )

                .notes(
                        inspection != null
                                ? inspection.getNotes()
                                : null
                )

                .inspected(
                        inspection != null
                )

                .build();
    }


    public ReceivingDetailResponse toDetailResponse(
            GoodsReceipts receipt,
            List<GoodsReceiptItems> items,
            List<ReceiptInspections> inspections
    ) {

        List<ReceivingItemResponse> responses =
                items.stream()

                        .map(item -> {

                            ReceiptInspections inspection =
                                    inspections.stream()

                                            .filter(i ->
                                                    i.getProduct()
                                                            .getId()
                                                            .equals(
                                                                    item.getProduct()
                                                                            .getId()
                                                            )
                                            )

                                            .findFirst()

                                            .orElse(null);

                            return toItemResponse(
                                    item,
                                    inspection
                            );
                        })

                        .toList();


        int totalExpected =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getExpectedQuantity
                        )
                        .sum();

        int totalActual =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getActualQuantity
                        )
                        .sum();

        int totalAccepted =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getAcceptedQuantity
                        )
                        .sum();

        int totalDamaged =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getDamagedQuantity
                        )
                        .sum();

        int totalShortage =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getShortageQuantity
                        )
                        .sum();

        int totalSurplus =
                items.stream()
                        .mapToInt(
                                GoodsReceiptItems::getSurplusQuantity
                        )
                        .sum();


        Users receivedBy =
                receipt.getReceivedBy();


        return ReceivingDetailResponse.builder()

                .receiptId(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus().name()
                )

                .warehouseId(
                        receipt.getWarehouse().getId()
                )

                .warehouseName(
                        receipt.getWarehouse().getName()
                )

                .purchaseOrderId(
                        receipt.getPurchaseOrder().getId()
                )

                .purchaseOrderCode(
                        receipt.getPurchaseOrder()
                                .getOrderCode()
                )

                .supplierId(
                        receipt.getPurchaseOrder()
                                .getSupplier()
                                .getId()
                )

                .supplierName(
                        receipt.getPurchaseOrder()
                                .getSupplier()
                                .getName()
                )

                .receivedBy(
                        receivedBy != null
                                ? receivedBy.getId()
                                : null
                )

                .receivedByName(
                        receivedBy != null
                                ? receivedBy.getName()
                                : null
                )

                .totalExpectedQuantity(
                        totalExpected
                )

                .totalActualQuantity(
                        totalActual
                )

                .totalAcceptedQuantity(
                        totalAccepted
                )

                .totalDamagedQuantity(
                        totalDamaged
                )

                .totalShortageQuantity(
                        totalShortage
                )

                .totalSurplusQuantity(
                        totalSurplus
                )

                .totalItems(
                        items.size()
                )

                .inspectedItems(
                        inspections.size()
                )

                .items(responses)

                .build();
    }
}