package com.toystorage.backend.services.packages;

import com.toystorage.backend.dto.response.packages.PackingConfirmationResponse;
import com.toystorage.backend.dto.response.packages.PackingPackageItemResponse;
import com.toystorage.backend.dto.response.packages.PackingPackageResponse;
import com.toystorage.backend.services.shipments.ShipmentManifestService;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.packages.PackageStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.transfers.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.StockTransferRepository;
import com.toystorage.backend.repository.packages.PackageItemRepository;
import com.toystorage.backend.repository.packages.PackageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackingConfirmationService {

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final PackageRepository
            packageRepository;

    private final PackageItemRepository
            packageItemRepository;

    private final PackingValidationService
            validationService;

    private final ShipmentManifestService
            shipmentManifestService;


    // =====================================================
    // VIEW PACKING RESULT
    // =====================================================

    @Transactional(readOnly = true)
    public PackingConfirmationResponse getPackingResult(
            Long transferId
    ) {

        Users manager =
                validationService
                        .getCurrentUser();

        StockTransfer transfer =
                validationService
                        .getTransfer(transferId);

        validationService.validateWarehouse(
                manager,
                transfer
        );

        List<PackageTransferItem> packages =
                validationService
                        .getTransferPackages(
                                transferId
                        );

        return buildResponse(
                transfer,
                packages,
                null
        );
    }


    // =====================================================
    // CONFIRM PACKING
    // =====================================================

    @Transactional
    public PackingConfirmationResponse confirmPacking(
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

        /*
         * Chỉ confirm khi đang ở PACKING.
         */
        if (transfer.getStatus()
                != TransferStatus.PACKING) {

            throw new BadRequest(
                    "Only PACKING stock transfer "
                            + "can be confirmed"
            );
        }

        List<PackageTransferItem> transferPackages =
                validationService
                        .getTransferPackages(
                                transferId
                        );

        /*
         * Validate từng package.
         */
        for (PackageTransferItem link
                : transferPackages) {

            Packages pack =
                    link.getPackageEntity();

            validationService.validatePackage(
                    pack
            );
        }

        /*
         * Kiểm tra số lượng từng product.
         */
        validateQuantities(
                transfer,
                transferPackages
        );


        /*
         * Manager check package.
         */
        for (PackageTransferItem link
                : transferPackages) {

            Packages pack =
                    link.getPackageEntity();

            pack.setCheckedBy(manager);

            pack.setStatus(
                    PackageStatus.CHECKED
            );

            pack.setUpdatedAt(
                    LocalDateTime.now()
            );

            packageRepository.save(pack);
        }


        /*
         * Transfer đóng gói hoàn tất.
         */
        transfer.setStatus(
             TransferStatus.PACKED
        );

        transfer.setUpdatedAt(
                LocalDateTime.now()
        );

        /*
         * Nếu confirmedBy/confirmedAt đang dùng
         * cho source confirmation khác,
         * KHÔNG nên overwrite.
         *
         * Tốt nhất có packingConfirmedBy/At.
         */

        stockTransferRepository.save(
                transfer
        );


        /*
         * Tạo bảng kê đi hàng.
         */
        ShipmentManifests manifest =
                shipmentManifestService
                        .createManifest(
                                transfer,
                                transferPackages,
                                manager
                        );

        return buildResponse(
                transfer,
                transferPackages,
                manifest
        );
    }


    // =====================================================
    // QUANTITY VALIDATION
    // =====================================================

    private void validateQuantities(
            StockTransfer transfer,
            List<PackageTransferItem> packages
    ) {

        List<StockTransferItems> transferItems =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transfer.getId()
                        );

        for (StockTransferItems transferItem
                : transferItems) {

            Long productId =
                    transferItem
                            .getProduct()
                            .getId();

            int packageQuantity = 0;

            for (PackageTransferItem link
                    : packages) {

                List<PackageItems> packageItems =
                        packageItemRepository
                                .findByPackageEntityId(
                                        link
                                                .getPackageEntity()
                                                .getId()
                                );

                packageQuantity +=
                        packageItems.stream()

                                .filter(item ->
                                        item.getProduct()
                                                .getId()
                                                .equals(
                                                        productId
                                                )
                                )

                                .mapToInt(
                                        PackageItems::getQuantity
                                )

                                .sum();
            }

            /*
             * So sánh với approved_quantity.
             */
            if (packageQuantity
                    != transferItem
                    .getApprovedQuantity()) {

                throw new BadRequest(
                        "Packed quantity mismatch for product "
                                + productId
                                + ". Expected: "
                                + transferItem
                                .getApprovedQuantity()
                                + ", packed: "
                                + packageQuantity
                );
            }

            /*
             * Đồng bộ packed_quantity.
             */
            transferItem.setPackedQuantity(
                    packageQuantity
            );

            stockTransferItemRepository
                    .save(transferItem);
        }
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private PackingConfirmationResponse buildResponse(
            StockTransfer transfer,
            List<PackageTransferItem> links,
            ShipmentManifests manifest
    ) {

        List<PackingPackageResponse> packageResponses =
                links.stream()

                        .map(link ->
                                buildPackageResponse(
                                        link.getPackageEntity()
                                )
                        )

                        .toList();

        int expected =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transfer.getId()
                        )

                        .stream()

                        .mapToInt(
                                StockTransferItems
                                        ::getApprovedQuantity
                        )

                        .sum();

        int packed =
                packageResponses.stream()
                        .mapToInt(
                                PackingPackageResponse
                                        ::getTotalQuantity
                        )
                        .sum();

        boolean allSealed =
                packageResponses.stream()
                        .allMatch(
                                PackingPackageResponse::getSealed
                        );

        boolean allPacked =
                packageResponses.stream()
                        .allMatch(p ->
                                "PACKED".equals(p.getStatus())
                                        ||
                                        "CHECKED".equals(p.getStatus())
                        );

        Users confirmedBy =
                transfer.getConfirmedBy();

        return PackingConfirmationResponse.builder()

                .transferId(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .transferStatus(
                        transfer.getStatus().name()
                )

                .expectedQuantity(expected)

                .packedQuantity(packed)

                .quantityMatched(
                        expected == packed
                )

                .allPackagesSealed(
                        allSealed
                )

                .allPackagesPacked(
                        allPacked
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


    private PackingPackageResponse buildPackageResponse(
            Packages pack
    ) {

        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                pack.getId()
                        );

        List<PackingPackageItemResponse> responses =
                items.stream()

                        .map(item ->
                                PackingPackageItemResponse
                                        .builder()

                                        .productId(
                                                item
                                                        .getProduct()
                                                        .getId()
                                        )

                                        .productName(
                                                item
                                                        .getProduct()
                                                        .getName()
                                        )

                                        .packedQuantity(
                                                item.getQuantity()
                                        )

                                        .build()
                        )

                        .toList();

        int quantity =
                items.stream()
                        .mapToInt(
                                PackageItems::getQuantity
                        )
                        .sum();

        Users packedBy =
                pack.getPackedBy();

        Users checkedBy =
                pack.getCheckedBy();

        return PackingPackageResponse.builder()

                .packageId(pack.getId())

                .packageCode(
                        pack.getPackagesCode()
                )

                .status(
                        pack.getStatus().name()
                )

                .sealNumber(
                        pack.getSealNumber()
                )

                .sealed(
                        pack.getSealNumber() != null
                                && !pack
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

                .totalQuantity(quantity)

                .items(responses)

                .build();
    }
}