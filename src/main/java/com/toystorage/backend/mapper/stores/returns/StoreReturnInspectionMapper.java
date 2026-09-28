package com.toystorage.backend.mapper.stores.returns;

import com.toystorage.backend.dto.response.inventories.discrepancy.DiscrepancyReportResponse;
import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionItemResponse;
import com.toystorage.backend.dto.response.stores.returns.StoreReturnInspectionResponse;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoreReturnInspectionMapper {


    // =====================================================
    // ITEM
    // =====================================================

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


    // =====================================================
    // DISCREPANCY
    // =====================================================

    public DiscrepancyReportResponse toDiscrepancyResponse(
            DiscrepancyReports report
    ) {

        return DiscrepancyReportResponse
                .builder()

                .id(
                        report.getId()
                )

                .reportCode(
                        report.getReportCode()
                )

                .discrepancyType(
                        report.getDiscrepancyType() != null
                                ? report.getDiscrepancyType().name()
                                : null
                )

                .status(
                        report.getStatus() != null
                                ? report.getStatus().name()
                                : null
                )

                .description(
                        report.getDescription()
                )

                /*
                 * DTO này trước đây thiết kế cho Goods Receipt.
                 * Store Return không sử dụng goodsReceiptId.
                 */
                .goodsReceiptId(
                        null
                )

                .productId(
                        report.getProductId()
                )

                .warehouseId(
                        report.getWarehouse() != null
                                ? report.getWarehouse().getId()
                                : null
                )

                .responsibleParty(
                        report.getResponsibleParty()
                )

                .resolutionAction(
                        report.getResolutionAction() != null
                                ? report.getResolutionAction().name()
                                : null
                )

                .resolutionNote(
                        report.getResolutionNote()
                )

                .reviewedBy(
                        report.getReviewedBy() != null
                                ? report.getReviewedBy().getId()
                                : null
                )

                .reviewedByName(
                        report.getReviewedBy() != null
                                ? report.getReviewedBy().getName()
                                : null
                )

                .reviewedAt(
                        report.getReviewedAt()
                )

                .resolvedBy(
                        report.getResolvedBy() != null
                                ? report.getResolvedBy().getId()
                                : null
                )

                .resolvedByName(
                        report.getResolvedBy() != null
                                ? report.getResolvedBy().getName()
                                : null
                )

                .resolvedAt(
                        report.getResolvedAt()
                )

                .createdAt(
                        report.getCreatedAt()
                )

                /*
                 * Store Return hiện không dùng
                 * discrepancy_items.
                 */
                .items(
                        List.of()
                )

                .build();
    }


    // =====================================================
    // RETURN RESPONSE
    // =====================================================

    public StoreReturnInspectionResponse toResponse(
            StoreReturns storeReturn,
            List<StoreReturnItems> items,
            List<DiscrepancyReports> discrepancies
    ) {

        int requested =
                items.stream()
                        .mapToInt(
                                item ->
                                        item.getRequestedQuantity() != null
                                                ? item.getRequestedQuantity()
                                                : 0
                        )
                        .sum();


        int received =
                items.stream()
                        .mapToInt(
                                item ->
                                        item.getReceivedQuantity() != null
                                                ? item.getReceivedQuantity()
                                                : 0
                        )
                        .sum();


        int approved =
                items.stream()
                        .mapToInt(
                                item ->
                                        item.getApprovedQuantity() != null
                                                ? item.getApprovedQuantity()
                                                : 0
                        )
                        .sum();


        int rejected =
                items.stream()
                        .mapToInt(
                                item ->
                                        item.getRejectedQuantity() != null
                                                ? item.getRejectedQuantity()
                                                : 0
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
                        storeReturn.getReturnType() != null
                                ? storeReturn.getReturnType().name()
                                : null
                )

                .status(
                        storeReturn.getStatus() != null
                                ? storeReturn.getStatus().name()
                                : null
                )

                .storeId(
                        storeReturn.getStore() != null
                                ? storeReturn.getStore().getId()
                                : null
                )

                .storeName(
                        storeReturn.getStore() != null
                                ? storeReturn.getStore().getName()
                                : null
                )

                .warehouseId(
                        storeReturn.getWarehouse() != null
                                ? storeReturn.getWarehouse().getId()
                                : null
                )

                .warehouseName(
                        storeReturn.getWarehouse() != null
                                ? storeReturn.getWarehouse().getName()
                                : null
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
                                .map(
                                        this::toItemResponse
                                )
                                .toList()
                )

                /*
                 * QUAN TRỌNG:
                 *
                 * Luôn có discrepancies trong JSON.
                 *
                 * Không có:
                 * "discrepancies": []
                 */
                .discrepancies(
                        discrepancies != null
                                ? discrepancies.stream()
                                .map(
                                        this::toDiscrepancyResponse
                                )
                                .toList()
                                : List.of()
                )

                .build();
    }
}