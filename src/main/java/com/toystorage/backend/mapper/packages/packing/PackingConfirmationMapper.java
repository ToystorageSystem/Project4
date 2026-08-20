package com.toystorage.backend.mapper.packages.packing;

import com.toystorage.backend.dto.response.packages.packing.PackingConfirmationResponse;
import com.toystorage.backend.dto.response.packages.packing.PackingPackageItemResponse;
import com.toystorage.backend.dto.response.packages.packing.PackingPackageResponse;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.shipments.ShipmentManifests;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.repository.packages.packing.PackageItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PackingConfirmationMapper {

    private final PackageItemRepository
            packageItemRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;


    // =====================================================
    // CONFIRMATION RESPONSE
    // =====================================================

    public PackingConfirmationResponse toResponse(
            StockTransfer transfer,
            List<PackageTransferItem> links,
            ShipmentManifests manifest
    ) {

        List<PackingPackageResponse> packageResponses =
                links.stream()
                        .map(link ->
                                toPackageResponse(
                                        link.getPackageEntity()
                                )
                        )
                        .toList();


        List<StockTransferItems> transferItems =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transfer.getId()
                        );


        int expectedQuantity =
                transferItems.stream()
                        .mapToInt(
                                StockTransferItems::getApprovedQuantity
                        )
                        .sum();


        int packedQuantity =
                packageResponses.stream()
                        .mapToInt(
                                PackingPackageResponse::getTotalQuantity
                        )
                        .sum();


        boolean allPackagesSealed =
                packageResponses.stream()
                        .allMatch(
                                PackingPackageResponse::getSealed
                        );


        boolean allPackagesPacked =
                packageResponses.stream()
                        .allMatch(packageResponse -> {

                            String status =
                                    packageResponse.getStatus();

                            return "PACKED".equals(status)
                                    || "CHECKED".equals(status);
                        });


        Users confirmedBy =
                transfer.getConfirmedBy();


        return PackingConfirmationResponse
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

                .expectedQuantity(
                        expectedQuantity
                )

                .packedQuantity(
                        packedQuantity
                )

                .quantityMatched(
                        expectedQuantity == packedQuantity
                )

                .allPackagesSealed(
                        allPackagesSealed
                )

                .allPackagesPacked(
                        allPackagesPacked
                )

                .confirmedBy(
                        confirmedBy != null
                                ? confirmedBy.getId()
                                : null
                )

                .confirmedByName(
                        confirmedBy != null
                                ? confirmedBy.getName()
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

                .packages(
                        packageResponses
                )

                .build();
    }


    // =====================================================
    // PACKAGE RESPONSE
    // =====================================================

    public PackingPackageResponse toPackageResponse(
            Packages packageEntity
    ) {

        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                packageEntity.getId()
                        );


        List<PackingPackageItemResponse> itemResponses =
                items.stream()
                        .map(this::toItemResponse)
                        .toList();


        int totalQuantity =
                items.stream()
                        .mapToInt(
                                PackageItems::getQuantity
                        )
                        .sum();


        Users packedBy =
                packageEntity.getPackedBy();

        Users checkedBy =
                packageEntity.getCheckedBy();


        return PackingPackageResponse
                .builder()

                .packageId(
                        packageEntity.getId()
                )

                .packageCode(
                        packageEntity.getPackagesCode()
                )

                .status(
                        packageEntity.getStatus() != null
                                ? packageEntity.getStatus().name()
                                : null
                )

                .sealNumber(
                        packageEntity.getSealNumber()
                )

                .sealed(
                        packageEntity.getSealNumber() != null
                                && !packageEntity
                                .getSealNumber()
                                .isBlank()
                )

                .packedBy(
                        packedBy != null
                                ? packedBy.getId()
                                : null
                )

                .packedByName(
                        packedBy != null
                                ? packedBy.getName()
                                : null
                )

                .checkedBy(
                        checkedBy != null
                                ? checkedBy.getId()
                                : null
                )

                .checkedByName(
                        checkedBy != null
                                ? checkedBy.getName()
                                : null
                )

                .totalQuantity(
                        totalQuantity
                )

                .items(
                        itemResponses
                )

                .build();
    }


    // =====================================================
    // ITEM RESPONSE
    // =====================================================

    public PackingPackageItemResponse toItemResponse(
            PackageItems item
    ) {

        return PackingPackageItemResponse
                .builder()

                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                )

                .productName(
                        item.getProduct() != null
                                ? item.getProduct().getName()
                                : null
                )

                .packedQuantity(
                        item.getQuantity()
                )

                .build();
    }
}