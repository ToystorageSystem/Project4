package com.toystorage.backend.services.packages.packing;

import com.toystorage.backend.dto.request.packages.packing.ScanPackageItemRequest;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingPackageResponse;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.packages.PackageStatus;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.mapper.packages.packing.StaffPackagePackingMapper;

import com.toystorage.backend.repository.packages.StaffPackageItemRepository;
import com.toystorage.backend.repository.packages.StaffPackageRepository;
import com.toystorage.backend.repository.packages.StaffPackageTransferItemRepository;

import com.toystorage.backend.repository.transfers.StockTransferItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffPackageItemService {

    private final StaffPackageRepository packageRepository;

    private final StaffPackageItemRepository
            packageItemRepository;

    private final StaffPackageTransferItemRepository
            packageTransferItemRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final StaffPackagePackingValidationService
            validationService;
    private final WarehouseTaskClaimService
            taskClaimService;


    private final StaffPackagePackingMapper mapper;


    @Transactional
    public StaffPackingPackageResponse scanItem(
            Long packageId,
            ScanPackageItemRequest request
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

        StockTransferItems transferItem =
                validationService.getTransferItem(
                        transfer.getId(),
                        request.getProductBarcode()
                );


        int alreadyPacked =
                getPackedQuantity(
                        transfer.getId(),
                        transferItem
                                .getProduct()
                                .getId()
                );


        validationService.validatePackedQuantity(
                transferItem,
                alreadyPacked,
                request.getQuantity()
        );


        if (packageEntity.getStatus()
                == PackageStatus.CREATED) {

            packageEntity.setStatus(
                    PackageStatus.PACKING
            );

            packageRepository.save(
                    packageEntity
            );
        }


        PackageItems packageItem =
                packageItemRepository
                        .findByPackageEntityIdAndProductId(
                                packageId,
                                transferItem
                                        .getProduct()
                                        .getId()
                        )

                        .orElseGet(() ->
                                PackageItems.builder()

                                        .packageEntity(
                                                packageEntity
                                        )

                                        .product(
                                                transferItem
                                                        .getProduct()
                                        )

                                        .quantity(0)

                                        .packageItemsCode(
                                                generateCode("PI")
                                        )

                                        .build()
                        );


        int currentQuantity =
                packageItem.getQuantity() == null
                        ? 0
                        : packageItem.getQuantity();


        packageItem.setQuantity(
                currentQuantity
                        + request.getQuantity()
        );


        packageItemRepository.save(
                packageItem
        );


        transferItem.setPackedQuantity(
                alreadyPacked
                        + request.getQuantity()
        );


        stockTransferItemRepository.save(
                transferItem
        );


        return buildResponse(
                packageEntity,
                transfer
        );
    }


    @Transactional(readOnly = true)
    public StaffPackingPackageResponse getPackage(
            Long packageId
    ) {

        Users staff =
                validationService.getCurrentUser();

        Packages packageEntity =
                validationService.getPackage(
                        packageId
                );

        StockTransfer transfer =
                getTransferOfPackage(
                        packageId
                );


        validationService.validateWarehouse(
                staff,
                transfer
        );


        return buildResponse(
                packageEntity,
                transfer
        );
    }


    private StaffPackingPackageResponse buildResponse(
            Packages packageEntity,
            StockTransfer transfer
    ) {

        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                packageEntity.getId()
                        );


        return mapper.toResponse(
                packageEntity,
                transfer,
                items
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


    private int getPackedQuantity(
            Long transferId,
            Long productId
    ) {

        Long quantity =
                packageItemRepository
                        .sumPackedQuantity(
                                transferId,
                                productId
                        );


        return quantity == null
                ? 0
                : Math.toIntExact(quantity);
    }


    private String generateCode(
            String prefix
    ) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}