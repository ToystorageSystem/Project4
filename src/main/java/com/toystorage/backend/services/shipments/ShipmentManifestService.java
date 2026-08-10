package com.toystorage.backend.services.shipments;


import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.packages.PackageTransferItem;

import com.toystorage.backend.enums.shipments.ShipmentManifestStatus;

import com.toystorage.backend.repository.shipments.ShipmentManifestPackageRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentManifestService {

    private final ShipmentManifestRepository
            shipmentManifestRepository;

    private final ShipmentManifestPackageRepository
            shipmentManifestPackageRepository;

    private final ShipmentManifestTransferRepository
            shipmentManifestTransferRepository;


    @Transactional
    public ShipmentManifests createManifest(
            StockTransfer transfer,
            List<PackageTransferItem> transferPackages,
            Users manager
    ) {

        ShipmentManifests manifest =
                new ShipmentManifests();

        manifest.setManifestCode(
                generateManifestCode()
        );

        manifest.setFromWarehouse(
                transfer.getFromWarehouse()
        );

        manifest.setToWarehouse(
                transfer.getToWarehouse()
        );

        manifest.setCreatedBy(manager);

        manifest.setStatus(
                ShipmentManifestStatus.CREATED
        );

        manifest.setCreatedAt(
                LocalDateTime.now()
        );

        manifest.setUpdatedAt(
                LocalDateTime.now()
        );

        ShipmentManifests saved =
                shipmentManifestRepository
                        .save(manifest);


        /*
         * LINK TRANSFER
         */
        ShipmentManifestTransfer mt =
                new ShipmentManifestTransfer();

        mt.setManifest(saved);

        mt.setTransfer(transfer);

        shipmentManifestTransferRepository
                .save(mt);


        /*
         * LINK PACKAGES
         */
        for (PackageTransferItem transferPackage
                : transferPackages) {

            ShipmentManifestPackage mp =
                    new ShipmentManifestPackage();

            mp.setManifest(saved);

            mp.setPackageEntity(
                    transferPackage.getPackageEntity()
            );

            shipmentManifestPackageRepository
                    .save(mp);
        }

        return saved;
    }


    private String generateManifestCode() {

        return "MAN-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}