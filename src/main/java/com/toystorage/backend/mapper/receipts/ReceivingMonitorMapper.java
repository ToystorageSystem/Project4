package com.toystorage.backend.mapper.receipts;

import com.toystorage.backend.dto.response.receipts.ReceivingIssueResponse;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import org.springframework.stereotype.Component;

@Component
public class ReceivingMonitorMapper {

    public ReceivingIssueResponse toIssueResponse(
            ReceiptInspections inspection
    ) {

        return ReceivingIssueResponse.builder()

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

                .inspectionResult(
                        inspection.getInspectedResult().name()
                )

                .notes(
                        inspection.getNotes()
                )

                .build();
    }
}