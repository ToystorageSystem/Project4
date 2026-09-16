package com.toystorage.backend.services.packages.shipments;

import com.toystorage.backend.dto.response.shipments.ShipmentHistoryResponse;

import com.toystorage.backend.entity.packages.PackageItems;

import com.toystorage.backend.entity.shipments.ShipmentManifestPackage;
import com.toystorage.backend.entity.shipments.ShipmentManifestTransfer;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.services.shipments.ShipmentValidationService;
import com.toystorage.backend.entity.transfers.StockTransfer;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.packages.packing.PackageItemRepository;

import com.toystorage.backend.repository.shipments.ShipmentManifestPackageRepository;
import com.toystorage.backend.repository.shipments.ShipmentManifestTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ShipmentHistoryService {

    private final ShipmentManifestTransferRepository
            shipmentManifestTransferRepository;

    private final ShipmentManifestPackageRepository
            shipmentManifestPackageRepository;

    private final PackageItemRepository
            packageItemRepository;

    private final ShipmentValidationService
            validationService;


    // =====================================================
    // SHIPMENT HISTORY
    // =====================================================

    @Transactional(readOnly = true)
    public Page<ShipmentHistoryResponse>
    getHistory(
            int page,
            int size,
            String keyword
    ) {

        Users manager =
                validationService
                        .getCurrentUser();


        if (manager.getWarehouse() == null) {

            throw new BadRequest(
                    "Manager is not assigned to a warehouse"
            );
        }


        int safePage =
                Math.max(
                        page,
                        0
                );


        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        24
                );


        String search =
                keyword == null
                        ? ""
                        : keyword.trim();


        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "manifest.createdAt"
                        )
                );


        Page<ShipmentManifestTransfer> history =
                shipmentManifestTransferRepository
                        .findShipmentHistory(
                                manager
                                        .getWarehouse()
                                        .getId(),

                                search,

                                pageable
                        );


        return history.map(
                this::mapToResponse
        );
    }


    // =====================================================
    // MAP RESPONSE
    // =====================================================

    private ShipmentHistoryResponse mapToResponse(
            ShipmentManifestTransfer link
    ) {

        ShipmentManifests manifest =
                link.getManifest();


        StockTransfer transfer =
                link.getTransfer();


        List<ShipmentManifestPackage>
                manifestPackages =
                shipmentManifestPackageRepository
                        .findByManifestId(
                                manifest.getId()
                        );


        int totalQuantity =
                manifestPackages
                        .stream()

                        .mapToInt(
                                manifestPackage -> {

                                    List<PackageItems>
                                            items =
                                            packageItemRepository
                                                    .findByPackageEntityId(
                                                            manifestPackage
                                                                    .getPackageEntity()
                                                                    .getId()
                                                    );


                                    return items
                                            .stream()

                                            .mapToInt(
                                                    item ->
                                                            item.getQuantity() == null
                                                                    ? 0
                                                                    : item.getQuantity()
                                            )

                                            .sum();
                                }
                        )

                        .sum();


        return ShipmentHistoryResponse
                .builder()

                .transferId(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .transferStatus(
                        transfer.getStatus() != null
                                ? transfer
                                .getStatus()
                                .name()
                                : null
                )


                .manifestId(
                        manifest.getId()
                )

                .manifestCode(
                        manifest.getManifestCode()
                )

                .manifestStatus(
                        manifest.getStatus() != null
                                ? manifest
                                .getStatus()
                                .name()
                                : null
                )


                .fromWarehouseId(
                        manifest.getFromWarehouse() != null
                                ? manifest
                                .getFromWarehouse()
                                .getId()
                                : null
                )

                .fromWarehouseName(
                        manifest.getFromWarehouse() != null
                                ? manifest
                                .getFromWarehouse()
                                .getName()
                                : null
                )


                .toWarehouseId(
                        manifest.getToWarehouse() != null
                                ? manifest
                                .getToWarehouse()
                                .getId()
                                : null
                )

                .toWarehouseName(
                        manifest.getToWarehouse() != null
                                ? manifest
                                .getToWarehouse()
                                .getName()
                                : null
                )


                .totalPackages(
                        manifestPackages.size()
                )

                .totalQuantity(
                        totalQuantity
                )


                .createdBy(
                        manifest.getCreatedBy() != null
                                ? manifest
                                .getCreatedBy()
                                .getId()
                                : null
                )

                .createdByName(
                        manifest.getCreatedBy() != null
                                ? manifest
                                .getCreatedBy()
                                .getName()
                                : null
                )


                .createdAt(
                        manifest.getCreatedAt()
                )


                .build();
    }
}