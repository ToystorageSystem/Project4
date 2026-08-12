package com.toystorage.backend.services.shipments;

import com.toystorage.backend.dto.request.shipments.ConfirmShipmentRequest;
import com.toystorage.backend.dto.response.shipments.*;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.shipments.*;
import com.toystorage.backend.entity.deliveries.Deliveries;
import com.toystorage.backend.entity.deliveries.DeliveryPackages;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.deliveries.DeliveryStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.packages.PackageItemRepository;
import com.toystorage.backend.repository.packages.PackageRepository;

import com.toystorage.backend.repository.deliveries.*;
import com.toystorage.backend.repository.shipments.*;
import com.toystorage.backend.repository.transfers.StockTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentConfirmationService {

    private final StockTransferRepository
            stockTransferRepository;

    private final PackageRepository
            packageRepository;

    private final PackageItemRepository
            packageItemRepository;

    private final ShipmentManifestRepository
            shipmentManifestRepository;

    private final ShipmentManifestPackageRepository
            shipmentManifestPackageRepository;

    private final ShipmentManifestTransferRepository
            shipmentManifestTransferRepository;

    private final DeliveryRepository
            deliveryRepository;

    private final DeliveryPackageRepository
            deliveryPackageRepository;

    private final ShipmentValidationService
            validationService;


    // =====================================================
    // VIEW SHIPMENT
    // =====================================================

    @Transactional(readOnly = true)
    public ShipmentConfirmationResponse getShipment(
            Long transferId
    ) {

        Users manager =
                validationService.getCurrentUser();

        StockTransfer transfer =
                validationService
                        .getTransfer(transferId);

        validationService.validateWarehouse(
                manager,
                transfer
        );

        ShipmentManifests manifest =
                getManifest(transferId);

        List<ShipmentManifestPackage> links =
                shipmentManifestPackageRepository
                        .findByManifestId(
                                manifest.getId()
                        );

        return buildResponse(
                transfer,
                manifest,
                null,
                links
        );
    }


    // =====================================================
    // CONFIRM SHIPMENT / HANDOVER
    // =====================================================

    @Transactional
    public ShipmentConfirmationResponse confirmShipment(
            Long transferId,
            ConfirmShipmentRequest request
    ) {

        Users manager =
                validationService.getCurrentUser();

        StockTransfer transfer =
                validationService
                        .getTransfer(transferId);

        validationService.validateWarehouse(
                manager,
                transfer
        );

        /*
         * Chỉ được xuất khi packing đã confirm.
         */
        if (transfer.getStatus()
                != TransferStatus.PACKED) {

            throw new BadRequest(
                    "Shipment can only be confirmed "
                            + "after packing is completed"
            );
        }

        ShipmentManifests manifest =
                getManifest(transferId);

        List<ShipmentManifestPackage> links =
                shipmentManifestPackageRepository
                        .findByManifestId(
                                manifest.getId()
                        );

        if (links.isEmpty()) {

            throw new BadRequest(
                    "Shipment manifest contains no packages"
            );
        }


        /*
         * Kiểm tra tất cả kiện.
         */
        for (ShipmentManifestPackage link
                : links) {

            Packages pack =
                    link.getPackageEntity();

            if (pack.getStatus()
                    != PackageStatus.CHECKED) {

                throw new BadRequest(
                        "Package "
                                + pack.getPackagesCode()
                                + " has not been checked"
                );
            }

            if (pack.getSealNumber() == null
                    || pack.getSealNumber().isBlank()) {

                throw new BadRequest(
                        "Package "
                                + pack.getPackagesCode()
                                + " is not sealed"
                );
            }
        }


        /*
         * Delivery phải được tạo và có driver.
         */
        Deliveries delivery =
                validationService
                        .getDelivery(
                                request.getDeliveryId()
                        );

        validationService.validateDelivery(
                delivery,
                transfer
        );


        /*
         * Không bàn giao hai lần.
         */
        if (delivery.getHandedOverAt() != null) {

            throw new BadRequest(
                    "Shipment has already been handed over"
            );
        }


        /*
         * Package -> SHIPPED
         */
        for (ShipmentManifestPackage link
                : links) {

            Packages pack =
                    link.getPackageEntity();

            pack.setStatus(
                    PackageStatus.SHIPPED
            );

            pack.setUpdatedAt(
                    LocalDateTime.now()
            );

            packageRepository.save(pack);
        }


        /*
         * Transfer -> SHIPPED
         */
        transfer.setStatus(
                TransferStatus.SHIPPED
        );

        transfer.setShippedAt(
                LocalDateTime.now()
        );

        transfer.setUpdatedAt(
                LocalDateTime.now()
        );

        stockTransferRepository.save(
                transfer
        );


        /*
         * Delivery -> IN_TRANSIT
         */
        delivery.setDeliveryStatus(
                DeliveryStatus.IN_TRANSIT
        );

        delivery.setStartedAt(
                LocalDateTime.now()
        );

        delivery.setHandedOverBy(
                manager
        );

        delivery.setHandedOverAt(
                LocalDateTime.now()
        );

        delivery.setUpdatedAt(
                LocalDateTime.now()
        );

        deliveryRepository.save(
                delivery
        );


        /*
         * Link packages với delivery.
         */
        createDeliveryPackages(
                delivery,
                links
        );


        return buildResponse(
                transfer,
                manifest,
                delivery,
                links
        );
    }


    // =====================================================
    // MANIFEST
    // =====================================================

    private ShipmentManifests getManifest(
            Long transferId
    ) {

        ShipmentManifestTransfer link =
                shipmentManifestTransferRepository
                        .findByTransferId(
                                transferId
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Shipment manifest not found "
                                                + "for transfer: "
                                                + transferId
                                )
                        );

        return link.getManifest();
    }


    // =====================================================
    // DELIVERY PACKAGE
    // =====================================================

    private void createDeliveryPackages(
            Deliveries delivery,
            List<ShipmentManifestPackage> links
    ) {

        for (ShipmentManifestPackage link
                : links) {

            DeliveryPackages deliveryPackage =
                    new DeliveryPackages();

            deliveryPackage.setShipmentPackagesCode(
                    "DP-"
                            + delivery.getId()
                            + "-"
                            + link.getPackageEntity().getId()
            );

            deliveryPackage.setDelivery(
                    delivery
            );

            deliveryPackage.setPackageEntity(
                    link.getPackageEntity()
            );

            deliveryPackageRepository.save(
                    deliveryPackage
            );
        }
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private ShipmentConfirmationResponse buildResponse(
            StockTransfer transfer,
            ShipmentManifests manifest,
            Deliveries delivery,
            List<ShipmentManifestPackage> links
    ) {

        List<ShipmentPackageResponse> packages =
                links.stream()

                        .map(link ->
                                buildPackageResponse(
                                        link.getPackageEntity()
                                )
                        )

                        .toList();

        Users handover =
                delivery != null
                        ? delivery.getHandedOverBy()
                        : null;

        Users driver =
                delivery != null
                        ? delivery.getDriver()
                        : null;

        return ShipmentConfirmationResponse
                .builder()

                .transferId(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .transferStatus(
                        transfer.getStatus().name()
                )

                .manifestId(
                        manifest.getId()
                )

                .manifestCode(
                        manifest.getManifestCode()
                )

                .manifestStatus(
                        manifest.getStatus().name()
                )

                .deliveryId(
                        delivery != null
                                ? delivery.getId()
                                : null
                )

                .shipmentCode(
                        delivery != null
                                ? delivery.getShipmentCode()
                                : null
                )

                .deliveryStatus(
                        delivery != null
                                ? delivery.getDeliveryStatus().name()
                                : null
                )

                .driverId(
                        driver != null
                                ? driver.getId()
                                : null
                )

                .driverName(
                        driver != null
                                ? driver.getName()
                                : null
                )

                .handedOverBy(
                        handover != null
                                ? handover.getId()
                                : null
                )

                .handedOverByName(
                        handover != null
                                ? handover.getName()
                                : null
                )

                .handedOverAt(
                        delivery != null
                                ? delivery.getHandedOverAt()
                                : null
                )

                .packages(packages)

                .build();
    }


    private ShipmentPackageResponse buildPackageResponse(
            Packages pack
    ) {

        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                pack.getId()
                        );

        int quantity =
                items.stream()
                        .mapToInt(
                                PackageItems::getQuantity
                        )
                        .sum();

        return ShipmentPackageResponse
                .builder()

                .packageId(
                        pack.getId()
                )

                .packageCode(
                        pack.getPackagesCode()
                )

                .sealNumber(
                        pack.getSealNumber()
                )

                .status(
                        pack.getStatus().name()
                )

                .totalQuantity(
                        quantity
                )

                .build();
    }
}