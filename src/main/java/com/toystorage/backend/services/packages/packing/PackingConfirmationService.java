package com.toystorage.backend.services.packages.packing;

import com.toystorage.backend.dto.response.packages.packing.PackingConfirmationResponse;
import com.toystorage.backend.mapper.packages.packing.PackingConfirmationMapper;
import com.toystorage.backend.services.shipments.ShipmentManifestService;
import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.repository.packages.packing.PackageTransferItemRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.packages.PackageStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;
import com.toystorage.backend.repository.packages.packing.PackageItemRepository;
import com.toystorage.backend.repository.packages.packing.PackageRepository;

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
    private final PackingConfirmationMapper
            mapper;
    private final PackageTransferItemRepository
            packageTransferItemRepository;
    // =====================================================
    // VIEW PACKING RESULT
    // =====================================================
    @Transactional(readOnly = true)
    public Page<PackingConfirmationResponse>
    getPackingConfirmations(
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
                                "createdAt"
                        )
                );


        Page<StockTransfer> transfers =
                stockTransferRepository
                        .findPackingConfirmations(
                                manager
                                        .getWarehouse()
                                        .getId(),

                                TransferStatus.PACKING,

                                search,

                                pageable
                        );


        return transfers.map(
                transfer -> {

                    List<PackageTransferItem>
                            packages =
                            packageTransferItemRepository
                                    .findByStockTransferId(
                                            transfer.getId()
                                    );


                    return mapper.toResponse(
                            transfer,
                            packages,
                            null
                    );
                }
        );
    }
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

        return mapper.toResponse(
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

        return mapper.toResponse(
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
}