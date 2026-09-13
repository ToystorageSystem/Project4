package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.enums.receipts.InspectionResult;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReceivingMonitorCalculator {

    public Map<Long, ReceiptInspections> mapInspectionByProduct(
            List<ReceiptInspections> inspections
    ) {

        return inspections
                .stream()
                .collect(
                        Collectors.toMap(
                                inspection ->
                                        inspection
                                                .getProduct()
                                                .getId(),
                                Function.identity(),
                                (first, second) -> second
                        )
                );
    }

    public int totalProducts(
            List<GoodsReceiptItems> items
    ) {

        return items.size();
    }

    public int inspectedProducts(
            Map<Long, ReceiptInspections> inspectionByProduct
    ) {

        return inspectionByProduct.size();
    }

    public int remainingProducts(
            int totalProducts,
            int inspectedProducts
    ) {

        return Math.max(
                totalProducts - inspectedProducts,
                0
        );
    }

    public int issueProducts(
            List<ReceiptInspections> inspections
    ) {

        return (int) inspections
                .stream()
                .filter(inspection ->
                        inspection.getInspectedResult() != null
                                && inspection.getInspectedResult()
                                != InspectionResult.MATCHED
                )
                .map(inspection ->
                        inspection
                                .getProduct()
                                .getId()
                )
                .distinct()
                .count();
    }

    public int totalExpectedQuantity(
            List<GoodsReceiptItems> items
    ) {

        return items
                .stream()
                .mapToInt(item ->
                        safeInteger(
                                item.getExpectedQuantity()
                        )
                )
                .sum();
    }

    public int totalActualQuantity(
            List<ReceiptInspections> inspections
    ) {

        return inspections
                .stream()
                .mapToInt(inspection ->
                        safeInteger(
                                inspection.getActualQuantity()
                        )
                )
                .sum();
    }

    public int totalDamagedQuantity(
            List<GoodsReceiptItems> items,
            Map<Long, ReceiptInspections> inspectionByProduct
    ) {

        return items
                .stream()
                .filter(item ->
                        inspectionByProduct
                                .containsKey(
                                        item
                                                .getProduct()
                                                .getId()
                                )
                )
                .mapToInt(item ->
                        safeInteger(
                                item.getDamagedQuantity()
                        )
                )
                .sum();
    }

    public int totalShortageQuantity(
            List<GoodsReceiptItems> items,
            Map<Long, ReceiptInspections> inspectionByProduct
    ) {

        return items
                .stream()
                .filter(item ->
                        inspectionByProduct
                                .containsKey(
                                        item
                                                .getProduct()
                                                .getId()
                                )
                )
                .mapToInt(item ->
                        safeInteger(
                                item.getShortageQuantity()
                        )
                )
                .sum();
    }

    public int totalSurplusQuantity(
            List<GoodsReceiptItems> items,
            Map<Long, ReceiptInspections> inspectionByProduct
    ) {

        return items
                .stream()
                .filter(item ->
                        inspectionByProduct
                                .containsKey(
                                        item
                                                .getProduct()
                                                .getId()
                                )
                )
                .mapToInt(item ->
                        safeInteger(
                                item.getSurplusQuantity()
                        )
                )
                .sum();
    }

    public int remainingQuantity(
            int expected,
            int actual
    ) {

        return Math.max(
                expected - actual,
                0
        );
    }

    public double progressPercent(
            int totalProducts,
            int inspectedProducts
    ) {

        if (totalProducts == 0) {
            return 0;
        }

        double progress =
                (
                        (double) inspectedProducts
                                / totalProducts
                ) * 100;

        progress =
                Math.min(
                        progress,
                        100
                );

        return Math.round(
                progress * 100.0
        ) / 100.0;
    }

    public int safeInteger(
            Integer value
    ) {

        return value != null
                ? value
                : 0;
    }
}
