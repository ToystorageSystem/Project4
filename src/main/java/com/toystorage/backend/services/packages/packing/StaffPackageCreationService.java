package com.toystorage.backend.services.packages.packing;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingPackageResponse;

import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;

import com.toystorage.backend.enums.packages.PackageStatus;

import com.toystorage.backend.mapper.packages.packing.StaffPackagePackingMapper;

import com.toystorage.backend.repository.packages.StaffPackageRepository;
import com.toystorage.backend.repository.packages.StaffPackageTransferItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffPackageCreationService {

    private final StaffPackageRepository packageRepository;

    private final StaffPackageTransferItemRepository
            packageTransferItemRepository;

    private final StaffPackagePackingValidationService
            validationService;

    private final WarehouseTaskClaimService
            taskClaimService;

    private final StaffPackagePackingMapper mapper;

    private final StaffPackagePrintService printService;


    @Transactional
    public StaffPackingPackageResponse createPackage(
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

        taskClaimService.claim(
                WarehouseTaskType.TRANSFER_PACKING,
                transferId,
                staff
        );




        Packages packageEntity =
                Packages.builder()

                        .packagesCode(
                                "TMP-" + UUID.randomUUID()
                        )

                        .fromWarehouse(
                                transfer.getFromWarehouse()
                        )

                        .toWarehouse(
                                transfer.getToWarehouse()
                        )

                        .packedBy(staff)

                        .status(
                                PackageStatus.CREATED
                        )

                        .build();


        packageEntity =
                packageRepository.saveAndFlush(
                        packageEntity
                );


        String destinationCode =
                "WH"
                        + transfer
                        .getToWarehouse()
                        .getId();


        String packageCode =
                String.format(
                        "PKG-%s-%06d",
                        sanitizeCode(destinationCode),
                        packageEntity.getId()
                );


        packageEntity.setPackagesCode(
                packageCode
        );


        packageEntity =
                packageRepository.save(
                        packageEntity
                );


        PackageTransferItem relation =
                PackageTransferItem.builder()

                        .packageEntity(
                                packageEntity
                        )

                        .stockTransfer(
                                transfer
                        )

                        .packageTransferItemsCode(
                                generateCode("PTI")
                        )

                        .build();


        packageTransferItemRepository.save(
                relation
        );


        printService.printPackageLabel(
                packageEntity,
                transfer
        );


        return mapper.toResponse(
                packageEntity,
                transfer,
                List.of()
        );
    }


    private String sanitizeCode(
            String value
    ) {

        return value
                .replaceAll(
                        "[^A-Za-z0-9]",
                        ""
                )
                .toUpperCase();
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