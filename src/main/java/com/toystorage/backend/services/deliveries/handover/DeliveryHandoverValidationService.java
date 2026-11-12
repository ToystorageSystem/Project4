package com.toystorage.backend.services.deliveries.handover;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.exceptions.BadRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryHandoverValidationService {

    public void validateCanHandover(
            Deliveries delivery,
            Users driver
    ) {

        /*
         * 2026-11-13: at a store the store staff scans the packages (store receiving F1);
         * the driver handover is only used for other destinations.
         */
        if (delivery.getToWarehouse() != null
                && delivery.getToWarehouse().getType()
                == com.toystorage.backend.enums.warehouses.WarehouseType.STORE) {
            throw new com.toystorage.backend.exceptions.BadRequest(
                    "Deliveries to a store are completed by the store: it scans the packages and finishes package receiving"
            );
        }


        if (delivery.getDeliveryStatus()
                != DeliveryStatus.ARRIVED) {

            throw new BadRequest(
                    "Delivery must be ARRIVED before handover"
            );
        }

        if (delivery.getDriver() == null
                || !delivery.getDriver()
                .getId()
                .equals(driver.getId())) {

            throw new BadRequest(
                    "This delivery is not assigned to you"
            );
        }
    }

    public void validateNotCompleted(
            com.toystorage.backend.entity.deliveries.DeliveryHandover handover
    ) {

        if (handover.getCompletedAt() != null) {

            throw new BadRequest(
                    "Delivery handover has already been completed"
            );
        }
    }
}