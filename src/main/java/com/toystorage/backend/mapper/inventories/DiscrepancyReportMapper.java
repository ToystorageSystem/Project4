package com.toystorage.backend.mapper.inventories;

import com.toystorage.backend.dto.response.inventories.DiscrepancyItemResponse;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import org.springframework.stereotype.Component;

@Component
public class DiscrepancyReportMapper {

    public DiscrepancyItemResponse toItemResponse(
            ReceiptInspections inspection
    ) {

        int difference =
                inspection.getActualQuantity()
                        - inspection.getExpectedQuantity();

        return DiscrepancyItemResponse.builder()

                .productId(
                        inspection.getProduct().getId()
                )

                .productName(
                        inspection.getProduct().getName()
                )

                .expectedQuantity(
                        inspection.getExpectedQuantity()
                )

                .actualQuantity(
                        inspection.getActualQuantity()
                )

                .differenceQuantity(
                        difference
                )

                .inspectionResult(
                        inspection.getInspectedResult().name()
                )

                .notes(
                        inspection.getNotes()
                )

                .build();
    }
}