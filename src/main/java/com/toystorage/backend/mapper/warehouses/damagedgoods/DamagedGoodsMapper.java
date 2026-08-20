package com.toystorage.backend.mapper.warehouses.damagedgoods;

import com.toystorage.backend.dto.response.warehouses.damagedgoods.DamagedGoodsItemResponse;
import com.toystorage.backend.dto.response.warehouses.damagedgoods.DamagedGoodsReportResponse;
import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.entity.users.Users;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DamagedGoodsMapper {

    public DamagedGoodsItemResponse toItemResponse(
            DamagedGoodsItems item
    ) {

        return DamagedGoodsItemResponse.builder()

                .itemId(
                        item.getId()
                )

                .productId(
                        item.getProduct().getId()
                )

                .productName(
                        item.getProduct().getName()
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

                .locationId(
                        item.getLocation() != null
                                ? item.getLocation().getId()
                                : null
                )

                .locationName(
                        item.getLocation() != null
                                ? item.getLocation().getName()
                                : null
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


    public DamagedGoodsReportResponse toReportResponse(
            DamagedGoodsReports report,
            List<DamagedGoodsItems> items
    ) {

        Users reportedBy =
                report.getReportedBy();

        Users reviewedBy =
                report.getReviewedBy();

        List<DamagedGoodsItemResponse> itemResponses =
                items.stream()
                        .map(this::toItemResponse)
                        .toList();

        return DamagedGoodsReportResponse.builder()

                .reportId(
                        report.getId()
                )

                .reportCode(
                        report.getReportCode()
                )

                .sourceType(
                        report.getSourceType() != null
                                ? report.getSourceType().name()
                                : null
                )

                .sourceId(
                        report.getSourceId()
                )

                .description(
                        report.getDescription()
                )

                .status(
                        report.getStatus() != null
                                ? report.getStatus().name()
                                : null
                )

                .warehouseId(
                        report.getWarehouse() != null
                                ? report.getWarehouse().getId()
                                : null
                )

                .reportedBy(
                        reportedBy != null
                                ? reportedBy.getId()
                                : null
                )

                .reportedByName(
                        reportedBy != null
                                ? reportedBy.getName()
                                : null
                )

                .reviewedBy(
                        reviewedBy != null
                                ? reviewedBy.getId()
                                : null
                )

                .reviewedByName(
                        reviewedBy != null
                                ? reviewedBy.getName()
                                : null
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
                        itemResponses
                )

                .build();
    }
}
