package com.toystorage.backend.services.packages.packing;

import com.toystorage.backend.dto.request.packages.packing.SealPackageRequest;
import com.toystorage.backend.dto.response.packages.packing.StaffPackingPackageResponse;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.packages.packing.StaffPackagePackingMapper;

import com.toystorage.backend.repository.packages.packing.StaffPackageItemRepository;
import com.toystorage.backend.repository.packages.packing.StaffPackageRepository;
import com.toystorage.backend.repository.packages.packing.StaffPackageTransferItemRepository;

import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffPackageCompletionService {

    private final StaffPackageRepository packageRepository;

    private final StaffPackageItemRepository
            packageItemRepository;

    private final StaffPackageTransferItemRepository
            packageTransferItemRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final StockTransferRepository
            stockTransferRepository;

    private final StaffPackagePackingValidationService
            validationService;

    private final WarehouseTaskClaimService
            taskClaimService;

    private final StaffPackagePackingMapper mapper;


    @Transactional
    public StaffPackingPackageResponse sealPackage(
            Long packageId,
            SealPackageRequest request
    ) {

        Users staff =
                validationService.getCurrentUser();

        Packages packageEntity =
                validationService.getPackage(
                        packageId
                );


        validationService.validatePackageEditable(
                packageEntity
        );


        StockTransfer transfer =
                getTransferOfPackage(
                        packageId
                );


        validationService.validateWarehouse(
                staff,
                transfer
        );

        validationService.validateTransferPacking(
                transfer
        );

        taskClaimService.validateOwner(
                WarehouseTaskType.TRANSFER_PACKING,
                transfer.getId(),
                staff
        );


        List<PackageItems> packageItems =
                packageItemRepository
                        .findByPackageEntityId(
                                packageId
                        );


        if (packageItems.isEmpty()) {

            throw new BadRequest(
                    "Cannot seal empty package"
            );
        }


        if (packageEntity.getSealNumber() != null
                && !packageEntity
                .getSealNumber()
                .isBlank()) {

            throw new BadRequest(
                    "Package has already been sealed"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();

        packageEntity.setSealNumber(
                request.getSealNumber()
        );

        packageEntity.setStatus(
                PackageStatus.PACKED
        );

        packageEntity.setSealedAt(
                now
        );

        packageEntity.setPackedAt(
                now
        );

        packageEntity.setUpdatedAt(
                now
        );

        packageRepository.save(
                packageEntity
        );

        return mapper.toResponse(
                packageEntity,
                transfer,
                packageItems
        );
    }


    @Transactional
    public void completePacking(
            Long transferId
    ) {

        Users staff =
                validationService.getCurrentUser();

        StockTransfer transfer =
                validationService.getTransfer(
                        transferId
                );


        validationService.validateWarehouse(
                staff,
                transfer
        );

        validationService.validateTransferPacking(
                transfer
        );

        taskClaimService.validateOwner(
                WarehouseTaskType.TRANSFER_PACKING,
                transferId,
                staff
        );

        List<StockTransferItems> transferItems =
                stockTransferItemRepository
                        .findByStockTransferId(
                                transferId
                        );


        if (transferItems.isEmpty()) {

            throw new BadRequest(
                    "Transfer contains no items"
            );
        }


        boolean incomplete =
                transferItems.stream()
                        .anyMatch(item -> {

                            int picked =
                                    item.getPickedQuantity() == null
                                            ? 0
                                            : item.getPickedQuantity();

                            int packed =
                                    item.getPackedQuantity() == null
                                            ? 0
                                            : item.getPackedQuantity();

                            return packed != picked;
                        });


        if (incomplete) {

            throw new BadRequest(
                    "Not all picked products have been packed"
            );
        }


        List<PackageTransferItem> relations =
                packageTransferItemRepository
                        .findByStockTransferId(
                                transferId
                        );


        if (relations.isEmpty()) {

            throw new BadRequest(
                    "Transfer has no packages"
            );
        }


        boolean invalidPackage =
                relations.stream()

                        .map(
                                PackageTransferItem::getPackageEntity
                        )

                        .anyMatch(packageEntity ->
                                packageEntity.getStatus()
                                        != PackageStatus.PACKED
                        );


        if (invalidPackage) {

            throw new BadRequest(
                    "All packages must be sealed before completing packing"
            );
        }


        transfer.setPackingCompletedBy(
                staff
        );

        transfer.setPackingCompletedAt(
                LocalDateTime.now()
        );

        transfer.setUpdatedAt(
                LocalDateTime.now()
        );


        stockTransferRepository.save(
                transfer
        );

        taskClaimService.release(
                WarehouseTaskType.TRANSFER_PACKING,
                transferId,
                staff
        );
    }


    private StockTransfer getTransferOfPackage(
            Long packageId
    ) {

        return packageTransferItemRepository
                .findFirstByPackageEntityId(
                        packageId
                )

                .map(
                        PackageTransferItem::getStockTransfer
                )

                .orElseThrow(() ->
                        new BadRequest(
                                "Package is not linked to stock transfer"
                        )
                );
    }
}