package com.toystorage.backend.mapper.packages.packing;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingSubmissionPackageResponse;
import com.toystorage.backend.dto.response.packages.packing.StaffPackingSubmissionResponse;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.transfers.StockTransfer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StaffPackingSubmissionMapper {

    public StaffPackingSubmissionPackageResponse toPackageResponse(
            Packages packageEntity,
            List<PackageItems> items
    ) {

        int total =
                items.stream()
                        .mapToInt(item ->
                                item.getQuantity() == null
                                        ? 0
                                        : item.getQuantity()
                        )
                        .sum();


        return StaffPackingSubmissionPackageResponse
                .builder()

                .packageId(
                        packageEntity.getId()
                )

                .packageCode(
                        packageEntity.getPackagesCode()
                )

                .packageStatus(
                        packageEntity.getStatus() != null
                                ? packageEntity.getStatus().name()
                                : null
                )

                .sealNumber(
                        packageEntity.getSealNumber()
                )

                .totalQuantity(
                        total
                )

                .build();
    }


    public StaffPackingSubmissionResponse toResponse(
            StockTransfer transfer,
            int totalPicked,
            int totalPacked,
            List<Packages> packages,
            Map<Long, List<PackageItems>> packageItemsMap
    ) {

        return StaffPackingSubmissionResponse
                .builder()

                .transferId(
                        transfer.getId()
                )

                .transferCode(
                        transfer.getTransferCode()
                )

                .status(
                        transfer.getStatus().name()
                )

                .fromWarehouseId(
                        transfer.getFromWarehouse().getId()
                )

                .fromWarehouseName(
                        transfer.getFromWarehouse().getName()
                )

                .toWarehouseId(
                        transfer.getToWarehouse().getId()
                )

                .toWarehouseName(
                        transfer.getToWarehouse().getName()
                )

                .totalPickedQuantity(
                        totalPicked
                )

                .totalPackedQuantity(
                        totalPacked
                )

                .totalPackages(
                        packages.size()
                )

                .completedByName(
                        transfer.getPackingCompletedBy() != null
                                ? transfer
                                .getPackingCompletedBy()
                                .getName()
                                : null
                )

                .completedAt(
                        transfer.getPackingCompletedAt()
                )

                .packages(
                        packages.stream()
                                .map(packageEntity ->
                                        toPackageResponse(
                                                packageEntity,
                                                packageItemsMap
                                                        .getOrDefault(
                                                                packageEntity.getId(),
                                                                List.of()
                                                        )
                                        )
                                )
                                .toList()
                )

                .build();
    }
}