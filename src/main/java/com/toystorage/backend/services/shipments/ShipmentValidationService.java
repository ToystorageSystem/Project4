package com.toystorage.backend.services.shipments;

import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.exceptions.*;
import com.toystorage.backend.repository.deliveries.DeliveryRepository;
import com.toystorage.backend.repository.transfers.StockTransferRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShipmentValidationService {

    private final UserRepository userRepository;
    private final StockTransferRepository stockTransferRepository;
    private final DeliveryRepository deliveryRepository;

    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }

    public StockTransfer getTransfer(Long transferId) {

        return stockTransferRepository
                .findById(transferId)
                .orElseThrow(() ->
                        new NotFound(
                                "Stock transfer not found: "
                                        + transferId
                        )
                );
    }

    public Deliveries getDelivery(Long deliveryId) {

        return deliveryRepository
                .findById(deliveryId)
                .orElseThrow(() ->
                        new NotFound(
                                "Delivery not found: "
                                        + deliveryId
                        )
                );
    }

    public void validateWarehouse(
            Users manager,
            StockTransfer transfer
    ) {

        if (manager.getWarehouse() == null) {
            throw new Forbidden(
                    "Manager is not assigned to a warehouse"
            );
        }

        if (!manager.getWarehouse()
                .getId()
                .equals(
                        transfer.getFromWarehouse().getId()
                )) {

            throw new Forbidden(
                    "You cannot confirm shipment from another warehouse"
            );
        }
    }

    public void validateDelivery(
            Deliveries delivery,
            StockTransfer transfer
    ) {

        if (!delivery.getFromWarehouse()
                .getId()
                .equals(
                        transfer.getFromWarehouse().getId()
                )) {

            throw new BadRequest(
                    "Delivery source warehouse does not match transfer"
            );
        }

        if (!delivery.getToWarehouse()
                .getId()
                .equals(
                        transfer.getToWarehouse().getId()
                )) {

            throw new BadRequest(
                    "Delivery destination warehouse does not match transfer"
            );
        }

        if (delivery.getDriver() == null) {
            throw new BadRequest(
                    "Delivery has no assigned driver"
            );
        }
    }
}