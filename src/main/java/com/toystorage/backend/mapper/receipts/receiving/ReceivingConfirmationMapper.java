package com.toystorage.backend.mapper.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingConfirmationItemResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingConfirmationResponse;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;

import com.toystorage.backend.entity.users.Users;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceivingConfirmationMapper {


    // =====================================================
    // RESPONSE
    // =====================================================

    public ReceivingConfirmationResponse toResponse(
            GoodsReceipts receipt,
            List<GoodsReceiptItems> items,
            List<ReceiptInspections> inspections
    ) {

        List<ReceivingConfirmationItemResponse> itemResponses =
                items.stream()
                        .map(item ->
                                toItemResponse(
                                        item,
                                        findInspection(
                                                item,
                                                inspections
                                        )
                                )
                        )
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


        Users staff =
                receipt.getReceivedBy();

        Users manager =
                receipt.getInspectionConfirmedBy();


        return ReceivingConfirmationResponse
                .builder()

                .receiptId(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus() != null
                                ? receipt.getStatus().name()
                                : null
                )

                .staffId(
                        staff != null
                                ? staff.getId()
                                : null
                )

                .staffName(
                        staff != null
                                ? staff.getName()
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

                .items(
                        itemResponses
                )

                .confirmedBy(
                        manager != null
                                ? manager.getId()
                                : null
                )

                .confirmedByName(
                        manager != null
                                ? manager.getName()
                                : null
                )

                .confirmedAt(
                        receipt.getInspectionConfirmedAt()
                )

                .build();
    }


    // =====================================================
    // ITEM
    // =====================================================

    private ReceivingConfirmationItemResponse toItemResponse(
            GoodsReceiptItems item,
            ReceiptInspections inspection
    ) {

        return ReceivingConfirmationItemResponse
                .builder()

                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                )

                .productName(
                        item.getProduct() != null
                                ? item.getProduct().getName()
                                : null
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

                .inspectionResult(
                        inspection != null
                                && inspection.getInspectedResult() != null
                                ? inspection
                                .getInspectedResult()
                                .name()
                                : null
                )

                .build();
    }


    // =====================================================
    // FIND INSPECTION
    // =====================================================

    private ReceiptInspections findInspection(
            GoodsReceiptItems item,
            List<ReceiptInspections> inspections
    ) {

        if (item.getProduct() == null) {
            return null;
        }


        Long productId =
                item.getProduct().getId();


        return inspections.stream()

                .filter(inspection ->
                        inspection.getProduct() != null
                                && inspection
                                .getProduct()
                                .getId()
                                .equals(productId)
                )

                .findFirst()

                .orElse(null);
    }
}