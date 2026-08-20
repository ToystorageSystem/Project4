package com.toystorage.backend.mapper.deliveries.trips;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripDetailResponse;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripListResponse;

import com.toystorage.backend.dto.response.deliveries.trips
        .DeliveryTripPackageResponse;

import com.toystorage.backend.entity.deliveries.Deliveries;

import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.shipments
        .ShipmentManifestPackage;

import com.toystorage.backend.entity.shipments
        .ShipmentManifests;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.entity.warehouses.Warehouses;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeliveryTripMapper {


    // =====================================================
    // LIST
    // =====================================================

    public DeliveryTripListResponse toListResponse(
            Deliveries delivery,
            int packageCount
    ) {

        Warehouses from =
                delivery.getFromWarehouse();

        Warehouses to =
                delivery.getToWarehouse();


        return DeliveryTripListResponse
                .builder()

                .deliveryId(
                        delivery.getId()
                )

                .shipmentCode(
                        delivery.getShipmentCode()
                )

                .status(
                        delivery.getDeliveryStatus() != null
                                ? delivery
                                .getDeliveryStatus()
                                .name()
                                : null
                )

                .fromLocationId(
                        from != null
                                ? from.getId()
                                : null
                )

                .fromLocationCode(
                        from != null
                                ? from.getWarehousesCode()
                                : null
                )

                .fromLocationName(
                        from != null
                                ? from.getName()
                                : null
                )

                .fromLocationType(
                        from != null
                                && from.getType() != null
                                ? from.getType().name()
                                : null
                )

                .fromAddress(
                        from != null
                                ? from.getAddress()
                                : null
                )

                .toLocationId(
                        to != null
                                ? to.getId()
                                : null
                )

                .toLocationCode(
                        to != null
                                ? to.getWarehousesCode()
                                : null
                )

                .toLocationName(
                        to != null
                                ? to.getName()
                                : null
                )

                .toLocationType(
                        to != null
                                && to.getType() != null
                                ? to.getType().name()
                                : null
                )

                .toAddress(
                        to != null
                                ? to.getAddress()
                                : null
                )

                .expectedPickupAt(
                        delivery.getExpectedPickupAt()
                )

                .expectedDeliveryAt(
                        delivery.getExpectedDeliveryAt()
                )

                .packageCount(
                        packageCount
                )

                .build();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    public DeliveryTripDetailResponse toDetailResponse(
            Deliveries delivery,
            List<ShipmentManifestPackage> manifestPackages
    ) {

        Warehouses from =
                delivery.getFromWarehouse();

        Warehouses to =
                delivery.getToWarehouse();

        Users driver =
                delivery.getDriver();

        ShipmentManifests manifest =
                delivery.getManifest();


        List<DeliveryTripPackageResponse> packages =
                manifestPackages == null
                        ? List.of()
                        : manifestPackages
                        .stream()
                        .map(this::toPackageResponse)
                        .toList();


        return DeliveryTripDetailResponse
                .builder()

                .deliveryId(
                        delivery.getId()
                )

                .shipmentCode(
                        delivery.getShipmentCode()
                )

                .status(
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

                .driverName(
                        driver != null
                                ? driver.getName()
                                : null
                )

                .fromLocationId(
                        from != null
                                ? from.getId()
                                : null
                )

                .fromLocationCode(
                        from != null
                                ? from.getWarehousesCode()
                                : null
                )

                .fromLocationName(
                        from != null
                                ? from.getName()
                                : null
                )

                .fromLocationType(
                        from != null
                                && from.getType() != null
                                ? from.getType().name()
                                : null
                )

                .fromAddress(
                        from != null
                                ? from.getAddress()
                                : null
                )

                .toLocationId(
                        to != null
                                ? to.getId()
                                : null
                )

                .toLocationCode(
                        to != null
                                ? to.getWarehousesCode()
                                : null
                )

                .toLocationName(
                        to != null
                                ? to.getName()
                                : null
                )

                .toLocationType(
                        to != null
                                && to.getType() != null
                                ? to.getType().name()
                                : null
                )

                .toAddress(
                        to != null
                                ? to.getAddress()
                                : null
                )

                .expectedPickupAt(
                        delivery.getExpectedPickupAt()
                )

                .expectedDeliveryAt(
                        delivery.getExpectedDeliveryAt()
                )

                .acceptedAt(
                        delivery.getAcceptedAt()
                )

                .rejectedAt(
                        delivery.getRejectedAt()
                )

                .rejectionReason(
                        delivery.getRejectionReason()
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

                .packageCount(
                        packages.size()
                )

                .packages(
                        packages
                )

                .build();
    }


    private DeliveryTripPackageResponse toPackageResponse(
            ShipmentManifestPackage link
    ) {

        Packages packageEntity =
                link.getPackageEntity();


        return DeliveryTripPackageResponse
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
                                ? packageEntity
                                .getStatus()
                                .name()
                                : null
                )

                .build();
    }
}