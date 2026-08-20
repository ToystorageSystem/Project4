package com.toystorage.backend.mapper.inventories.shortage;

import com.toystorage.backend.dto.response.inventories.shortage.ReceivingShortageItemResponse;
import com.toystorage.backend.dto.response.inventories.shortage.ReceivingShortageReportResponse;
import com.toystorage.backend.entity.inventories.DiscrepancyItems;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReceivingShortageMapper {

    public ReceivingShortageItemResponse
    toItemResponse(
            DiscrepancyItems item
    ) {

        return ReceivingShortageItemResponse
                .builder()

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

                .shortageQuantity(
                        Math.abs(
                                item.getDifferenceQuantity()
                        )
                )

                .build();
    }


    public ReceivingShortageReportResponse
    toResponse(
            DiscrepancyReports report,
            List<DiscrepancyItems> items
    ) {

        return ReceivingShortageReportResponse
                .builder()

                .reportId(
                        report.getId()
                )

                .reportCode(
                        report.getReportCode()
                )

                .status(
                        report.getStatus().name()
                )

                .receiptId(
                        report.getReferenceId()
                )

                .warehouseId(
                        report.getWarehouse().getId()
                )

                .warehouseName(
                        report.getWarehouse().getName()
                )

                .reportedBy(
                        report.getReportedBy().getId()
                )

                .reportedByName(
                        report.getReportedBy().getName()
                )

                .description(
                        report.getDescription()
                )

                .createdAt(
                        report.getCreatedAt()
                )

                .items(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}