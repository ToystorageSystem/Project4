package com.toystorage.backend.mapper.warehouses;

import com.toystorage.backend.dto.response.warehouses.StaffDamagedGoodsItemResponse;
import com.toystorage.backend.dto.response.warehouses.StaffDamagedGoodsReportResponse;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffDamagedGoodsMapper {


    // =====================================================
    // ITEM
    // =====================================================

    public StaffDamagedGoodsItemResponse toItemResponse(
            DamagedGoodsItems item
    ) {

        return StaffDamagedGoodsItemResponse
                .builder()

                .itemId(
                        item.getId()
                )

                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                )

                .productCode(
                        item.getProduct() != null
                                ? item.getProduct()
                                .getProductsCode()
                                : null
                )

                .productName(
                        item.getProduct() != null
                                ? item.getProduct().getName()
                                : null
                )

                .barcode(
                        item.getProduct() != null
                                ? item.getProduct().getBarcode()
                                : null
                )

                .locationId(
                        item.getLocation() != null
                                ? item.getLocation().getId()
                                : null
                )

                .locationCode(
                        item.getLocation() != null
                                ? item.getLocation()
                                .getWarehouseCode()
                                : null
                )

                .locationName(
                        item.getLocation() != null
                                ? item.getLocation().getName()
                                : null
                )

                .quantity(
                        item.getQuantity()
                )

                .damageType(
                        item.getDamageType() != null
                                ? item.getDamageType().name()
                                : null
                )

                .conditionNote(
                        item.getConditionNote()
                )

                .evidenceImageUrl(
                        item.getEvidenceImage()
                )

                .disposition(
                        item.getDisposition() != null
                                ? item.getDisposition().name()
                                : null
                )

                .status(
                        item.getStatus() != null
                                ? item.getStatus().name()
                                : null
                )

                .build();
    }


    // =====================================================
    // REPORT
    // =====================================================

    public StaffDamagedGoodsReportResponse toResponse(
            DamagedGoodsReports report,
            List<DamagedGoodsItems> items
    ) {

        return StaffDamagedGoodsReportResponse
                .builder()

                .reportId(
                        report.getId()
                )

                .reportCode(
                        report.getReportCode()
                )

                .status(
                        report.getStatus() != null
                                ? report.getStatus().name()
                                : null
                )

                .sourceType(
                        report.getSourceType() != null
                                ? report.getSourceType().name()
                                : null
                )

                .sourceId(
                        report.getSourceId()
                )

                .warehouseId(
                        report.getWarehouse() != null
                                ? report.getWarehouse().getId()
                                : null
                )

                .warehouseName(
                        report.getWarehouse() != null
                                ? report.getWarehouse().getName()
                                : null
                )

                .reportedById(
                        report.getReportedBy() != null
                                ? report.getReportedBy().getId()
                                : null
                )

                .reportedByName(
                        report.getReportedBy() != null
                                ? report.getReportedBy().getName()
                                : null
                )

                .description(
                        report.getDescription()
                )

                .createdAt(
                        report.getCreatedAt()
                )

                .reviewedAt(
                        report.getReviewedAt()
                )

                .resolvedAt(
                        report.getResolvedAt()
                )

                .items(
                        items == null
                                ? List.of()
                                : items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}
