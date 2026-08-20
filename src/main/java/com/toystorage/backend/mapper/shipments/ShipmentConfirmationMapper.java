package com.toystorage.backend.mapper.shipments;

import com.toystorage.backend.dto.response.shipments.ShipmentConfirmationResponse;
import com.toystorage.backend.dto.response.shipments.ShipmentPackageResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import com.toystorage.backend.entity.shipments.ShipmentManifests;

import com.toystorage.backend.entity.transfers.StockTransfer;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.repository.packages.packing.PackageItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ShipmentConfirmationMapper {

    private final PackageItemRepository
            packageItemRepository;


    // =====================================================
    // SHIPMENT
    // =====================================================

    public ShipmentConfirmationResponse toResponse(
            StockTransfer transfer,
            ShipmentManifests manifest,
            Deliveries delivery,
            List<ShipmentManifestPackage> links
    ) {

        List<ShipmentPackageResponse> packages =
                links.stream()
                        .map(link ->
                                toPackageResponse(
                                        link.getPackageEntity()
                                )
                        )
                        .toList();


        Users handedOverBy =
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
                        transfer.getStatus() != null
                                ? transfer.getStatus().name()
                                : null
                )

                .manifestId(
                        manifest != null
                                ? manifest.getId()
                                : null
                )

                .manifestCode(
                        manifest != null
                                ? manifest.getManifestCode()
                                : null
                )

                .manifestStatus(
                        manifest != null
                                && manifest.getStatus() != null
                                ? manifest.getStatus().name()
                                : null
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
                                && delivery.getDeliveryStatus() != null
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

                .driverName(
                        driver != null
                                ? driver.getName()
                                : null
                )

                .handedOverBy(
                        handedOverBy != null
                                ? handedOverBy.getId()
                                : null
                )

                .handedOverByName(
                        handedOverBy != null
                                ? handedOverBy.getName()
                                : null
                )

                .handedOverAt(
                        delivery != null
                                ? delivery.getHandedOverAt()
                                : null
                )

                .packages(
                        packages
                )

                .build();
    }


    // =====================================================
    // PACKAGE
    // =====================================================

    public ShipmentPackageResponse toPackageResponse(
            Packages packageEntity
    ) {

        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                packageEntity.getId()
                        );


        int totalQuantity =
                items.stream()
                        .mapToInt(
                                PackageItems::getQuantity
                        )
                        .sum();


        return ShipmentPackageResponse
                .builder()

                .packageId(
                        packageEntity.getId()
                )

                .packageCode(
                        packageEntity.getPackagesCode()
                )

                .sealNumber(
                        packageEntity.getSealNumber()
                )

                .status(
                        packageEntity.getStatus() != null
                                ? packageEntity.getStatus().name()
                                : null
                )

                .totalQuantity(
                        totalQuantity
                )

                .build();
    }
}