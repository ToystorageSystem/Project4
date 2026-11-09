package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.dto.response.receipts.receiving.ReceivingIssueResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingMonitorResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProductProgressResponse;
import com.toystorage.backend.dto.response.receipts.receiving.ReceivingProgressResponse;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;

import com.toystorage.backend.enums.receipts.InspectionResult;

import com.toystorage.backend.mapper.receipts.receiving.ReceivingMonitorMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReceivingMonitorResponseBuilder {

    private final ReceivingMonitorQueryService queryService;
    private final ReceivingMonitorCalculator calculator;
    private final ReceivingMonitorMapper receivingMonitorMapper;

    public ReceivingProgressResponse buildProgressResponse(
            GoodsReceipts receipt
    ) {

        List<GoodsReceiptItems> items =
                queryService
                        .getItems(
                                receipt.getId()
                        );

        WarehouseTaskClaim receivingClaim =
                queryService
                        .resolveReceivingClaim(
                                receipt
                        );

        List<ReceiptInspections> inspections =
                queryService
                        .getInspectionsForClaim(
                                receivingClaim
                        );

        Map<Long, ReceiptInspections> inspectionByProduct =
                calculator
                        .mapInspectionByProduct(
                                inspections
                        );

        List<ReceivingProductProgressResponse> products =
                items
                        .stream()
                        .map(item ->
                                buildProductProgress(
                                        item,
                                        inspectionByProduct
                                )
                        )
                        .toList();

        int totalProducts =
                calculator
                        .totalProducts(
                                items
                        );

        int inspectedProducts =
                calculator
                        .inspectedProducts(
                                inspectionByProduct
                        );

        int remainingProducts =
                calculator
                        .remainingProducts(
                                totalProducts,
                                inspectedProducts
                        );

        int totalExpectedQuantity =
                calculator
                        .totalExpectedQuantity(
                                items
                        );

        int totalActualQuantity =
                calculator
                        .totalActualQuantity(
                                inspections
                        );

        int totalDamagedQuantity =
                calculator
                        .totalDamagedQuantity(
                                items,
                                inspectionByProduct
                        );

        int totalShortageQuantity =
                calculator
                        .totalShortageQuantity(
                                items,
                                inspectionByProduct
                        );

        int totalSurplusQuantity =
                calculator
                        .totalSurplusQuantity(
                                items,
                                inspectionByProduct
                        );

        double progress =
                calculator
                        .progressPercent(
                                totalProducts,
                                inspectedProducts
                        );

        List<ReceivingIssueResponse> issues =
                inspections
                        .stream()
                        .filter(inspection ->
                                inspection.getInspectedResult() != null
                                        && inspection.getInspectedResult()
                                        != InspectionResult.MATCHED
                        )
                        .map(
                                receivingMonitorMapper::toIssueResponse
                        )
                        .toList();

        Users staff =
                receivingClaim != null
                        ? receivingClaim.getClaimedBy()
                        : null;

        return ReceivingProgressResponse
                .builder()

                .receiptId(
                        receipt.getId()
                )

                .receiptCode(
                        receipt.getReceiptCode()
                )

                .status(
                        receipt.getStatus().name()
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

                .totalProducts(
                        totalProducts
                )

                .inspectedProducts(
                        inspectedProducts
                )

                .remainingProducts(
                        remainingProducts
                )

                .totalExpectedQuantity(
                        totalExpectedQuantity
                )

                .totalActualQuantity(
                        totalActualQuantity
                )

                .totalDamagedQuantity(
                        totalDamagedQuantity
                )

                .totalShortageQuantity(
                        totalShortageQuantity
                )

                .totalSurplusQuantity(
                        totalSurplusQuantity
                )

                .receivingStartedAt(
                        receivingClaim != null
                                ? receivingClaim.getClaimedAt()
                                : null
                )

                .inspectionCompletedAt(
                        receivingClaim != null
                                ? receivingClaim.getReleasedAt()
                                : null
                )

                .progressPercent(
                        progress
                )

                .products(
                        products
                )

                .issues(
                        issues
                )

                .build();
    }

    public ReceivingMonitorResponse buildMonitorResponse(
            GoodsReceipts receipt
    ) {

        List<GoodsReceiptItems> items =
                queryService
                        .getItems(
                                receipt.getId()
                        );

        WarehouseTaskClaim receivingClaim =
                queryService
                        .resolveReceivingClaim(
                                receipt
                        );

        List<ReceiptInspections> inspections =
                queryService
                        .getInspectionsForClaim(
                                receivingClaim
                        );

        Map<Long, ReceiptInspections> inspectionByProduct =
                calculator
                        .mapInspectionByProduct(
                                inspections
                        );

        int totalProducts =
                calculator
                        .totalProducts(
                                items
                        );

        int inspectedProducts =
                calculator
                        .inspectedProducts(
                                inspectionByProduct
                        );

        int remainingProducts =
                calculator
                        .remainingProducts(
                                totalProducts,
                                inspectedProducts
                        );

        int issueProducts =
                calculator
                        .issueProducts(
                                inspections
                        );

        int totalExpectedQuantity =
                calculator
                        .totalExpectedQuantity(
                                items
                        );

        int totalActualQuantity =
                calculator
                        .totalActualQuantity(
                                inspections
                        );

        int remainingQuantity =
                calculator
                        .remainingQuantity(
                                totalExpectedQuantity,
                                totalActualQuantity
                        );

        double progress =
                calculator
                        .progressPercent(
                                totalProducts,
                                inspectedProducts
                        );

        Users staff =
                receivingClaim != null
                        ? receivingClaim.getClaimedBy()
                        : null;

        return ReceivingMonitorResponse
                .builder()

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
                        receipt
                                .getWarehouse()
                                .getId()
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

                .totalProducts(
                        totalProducts
                )

                .inspectedProducts(
                        inspectedProducts
                )

                .remainingProducts(
                        remainingProducts
                )

                .issueProducts(
                        issueProducts
                )

                .totalExpectedQuantity(
                        totalExpectedQuantity
                )

                .totalActualQuantity(
                        totalActualQuantity
                )

                .remainingQuantity(
                        remainingQuantity
                )

                .progressPercent(
                        progress
                )

                .receivingStartedAt(
                        receivingClaim != null
                                ? receivingClaim.getClaimedAt()
                                : null
                )

                .build();
    }

    private ReceivingProductProgressResponse buildProductProgress(
            GoodsReceiptItems item,
            Map<Long, ReceiptInspections> inspectionByProduct
    ) {

        Long productId =
                item
                        .getProduct()
                        .getId();

        ReceiptInspections inspection =
                inspectionByProduct
                        .get(
                                productId
                        );

        boolean inspected =
                inspection != null;

        return ReceivingProductProgressResponse
                .builder()

                .productId(
                        productId
                )

                .productCode(
                        item
                                .getProduct()
                                .getProductsCode()
                )

                .productName(
                        item
                                .getProduct()
                                .getName()
                )

                .barcode(
                        item
                                .getProduct()
                                .getBarcode()
                )

                .expectedQuantity(
                        item.getExpectedQuantity()
                )

                .actualQuantity(
                        inspected
                                ? inspection.getActualQuantity()
                                : null
                )

                .acceptedQuantity(
                        inspected
                                ? item.getAcceptedQuantity()
                                : null
                )

                .damagedQuantity(
                        inspected
                                ? item.getDamagedQuantity()
                                : null
                )

                .shortageQuantity(
                        inspected
                                ? item.getShortageQuantity()
                                : null
                )

                .surplusQuantity(
                        inspected
                                ? item.getSurplusQuantity()
                                : null
                )

                .inspected(
                        inspected
                )

                .inspectionResult(
                        inspected
                                && inspection.getInspectedResult() != null
                                ? inspection.getInspectedResult().name()
                                : null
                )

                .packageCode(
                        inspected
                                ? inspection.getPackageCode()
                                : null
                )

                .evidenceImage(
                        inspected
                                ? inspection.getEvidenceImage()
                                : null
                )

                .notes(
                        inspected
                                ? inspection.getNotes()
                                : null
                )

                .inspectedById(
                        inspected
                                && inspection.getInspectedBy() != null
                                ? inspection.getInspectedBy().getId()
                                : null
                )

                .inspectedByName(
                        inspected
                                && inspection.getInspectedBy() != null
                                ? inspection.getInspectedBy().getName()
                                : null
                )

                .inspectedAt(
                        inspected
                                ? inspection.getInspectedAt()
                                : null
                )

                .build();
    }
}
