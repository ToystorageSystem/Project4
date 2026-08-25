package com.toystorage.backend.mapper.deliveries.handover;

import com.toystorage.backend.dto.response.deliveries.handover.*;
import com.toystorage.backend.entity.deliveries.*;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeliveryHandoverMapper {

    public DeliveryHandoverItemResponse toItemResponse(
            DeliveryHandoverItem item
    ) {

        return DeliveryHandoverItemResponse.builder()

                .packageId(
                        item.getPackageEntity().getId()
                )

                .packageCode(
                        item.getPackageEntity().getPackagesCode()
                )

                .expectedSealNumber(
                        item.getPackageEntity().getSealNumber()
                )

                .actualSealNumber(
                        item.getActualSealNumber()
                )

                .condition(
                        item.getCondition().name()
                )

                .issueNote(
                        item.getIssueNote()
                )

                .scannedAt(
                        item.getScannedAt()
                )

                .build();
    }


    public DeliveryHandoverResponse toResponse(
            DeliveryHandover handover,
            List<DeliveryHandoverItem> items,
            int expectedCount,
            int missingCount,
            int abnormalCount
    ) {

        Deliveries delivery =
                handover.getDelivery();

        return DeliveryHandoverResponse.builder()

                .handoverId(
                        handover.getId()
                )

                .deliveryId(
                        delivery.getId()
                )

                .shipmentCode(
                        delivery.getShipmentCode()
                )

                .deliveryStatus(
                        delivery.getDeliveryStatus().name()
                )

                .expectedPackageCount(
                        expectedCount
                )

                .handedOverPackageCount(
                        items.size()
                )

                .missingPackageCount(
                        missingCount
                )

                .abnormalPackageCount(
                        abnormalCount
                )

                .handedOverById(
                        handover.getHandedOverBy().getId()
                )

                .handedOverByName(
                        handover.getHandedOverBy().getName()
                )

                .receivedById(
                        handover.getReceivedBy() != null
                                ? handover.getReceivedBy().getId()
                                : null
                )

                .receivedByName(
                        handover.getReceivedBy() != null
                                ? handover.getReceivedBy().getName()
                                : null
                )

                .note(
                        handover.getNote()
                )

                .completedAt(
                        handover.getCompletedAt()
                )

                .packages(
                        items.stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}