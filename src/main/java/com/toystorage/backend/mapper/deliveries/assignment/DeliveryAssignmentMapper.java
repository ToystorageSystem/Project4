package com.toystorage.backend.mapper.deliveries.assignment;

import com.toystorage.backend.dto.response.deliveries.assignment
        .DeliveryAssignmentResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries
        .DeliveryAssignmentHistory;

import com.toystorage.backend.entity.users.Users;

import org.springframework.stereotype.Component;

@Component
public class DeliveryAssignmentMapper {


    public DeliveryAssignmentResponse toResponse(
            Deliveries delivery,
            DeliveryAssignmentHistory history
    ) {

        Users driver =
                delivery.getDriver();


        return DeliveryAssignmentResponse
                .builder()

                .deliveryId(
                        delivery.getId()
                )

                .shipmentCode(
                        delivery.getShipmentCode()
                )

                .deliveryStatus(
                        delivery.getDeliveryStatus() != null
                                ? delivery
                                .getDeliveryStatus()
                                .name()
                                : null
                )


                .driverId(
                        driver != null
                                ? driver.getId()
                                : null
                )

                .driverCode(
                        driver != null
                                ? driver.getUserCode()
                                : null
                )

                .driverName(
                        driver != null
                                ? driver.getName()
                                : null
                )

                .driverEmail(
                        driver != null
                                ? driver.getEmail()
                                : null
                )


                .assignmentHistoryId(
                        history != null
                                ? history.getId()
                                : null
                )

                .assignedAt(
                        history != null
                                ? history.getAssignedAt()
                                : null
                )

                .acceptedAt(
                        history != null
                                ? history.getAcceptedAt()
                                : null
                )

                .rejectedAt(
                        history != null
                                ? history.getRejectedAt()
                                : null
                )

                .rejectionReason(
                        history != null
                                ? history.getRejectionReason()
                                : null
                )


                .fromWarehouseId(
                        delivery.getFromWarehouse() != null
                                ? delivery
                                .getFromWarehouse()
                                .getId()
                                : null
                )

                .fromWarehouseName(
                        delivery.getFromWarehouse() != null
                                ? delivery
                                .getFromWarehouse()
                                .getName()
                                : null
                )


                .toWarehouseId(
                        delivery.getToWarehouse() != null
                                ? delivery
                                .getToWarehouse()
                                .getId()
                                : null
                )

                .toWarehouseName(
                        delivery.getToWarehouse() != null
                                ? delivery
                                .getToWarehouse()
                                .getName()
                                : null
                )


                .expectedPickupAt(
                        delivery.getExpectedPickupAt()
                )

                .expectedDeliveryAt(
                        delivery.getExpectedDeliveryAt()
                )

                .build();
    }
}