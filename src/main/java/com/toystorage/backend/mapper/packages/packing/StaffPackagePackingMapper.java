package com.toystorage.backend.mapper.packages.packing;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingPackageItemResponse;
import com.toystorage.backend.dto.response.packages.packing.StaffPackingPackageResponse;

import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.transfers.StockTransfer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffPackagePackingMapper {

    public StaffPackingPackageItemResponse toItemResponse(
            PackageItems item
    ) {

        return StaffPackingPackageItemResponse
                .builder()

                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null
                )

                .productCode(
                        item.getProduct() != null
                                ? item.getProduct().getProductsCode()
                                : null
                )

                .productName(
                        item.getProduct() != null
                                ? item.getProduct().getName()
                                : null
                )

                .barcode(
                        item.getProduct() != null
                                ? item.getProduct().getBarcode()
                                : null
                )

                .quantity(
                        item.getQuantity() != null
                                ? item.getQuantity()
                                : 0
                )

                .build();
    }


    public StaffPackingPackageResponse toResponse(
            Packages packageEntity,
            StockTransfer transfer,
            List<PackageItems> items
    ) {

        List<PackageItems> safeItems =
                items == null
                        ? List.of()
                        : items;


        int totalQuantity =
                safeItems
                        .stream()
                        .mapToInt(item ->
                                item.getQuantity() != null
                                        ? item.getQuantity()
                                        : 0
                        )
                        .sum();


        return StaffPackingPackageResponse
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

                .transferId(
                        transfer != null
                                ? transfer.getId()
                                : null
                )

                .transferCode(
                        transfer != null
                                ? transfer.getTransferCode()
                                : null
                )

                .fromWarehouseId(
                        transfer != null
                                && transfer.getFromWarehouse() != null
                                ? transfer.getFromWarehouse().getId()
                                : null
                )

                .fromWarehouseName(
                        transfer != null
                                && transfer.getFromWarehouse() != null
                                ? transfer.getFromWarehouse().getName()
                                : null
                )

                .toWarehouseId(
                        transfer != null
                                && transfer.getToWarehouse() != null
                                ? transfer.getToWarehouse().getId()
                                : null
                )

                .toWarehouseName(
                        transfer != null
                                && transfer.getToWarehouse() != null
                                ? transfer.getToWarehouse().getName()
                                : null
                )

                .sealNumber(
                        packageEntity.getSealNumber()
                )

                .totalQuantity(
                        totalQuantity
                )

                .packedByName(
                        packageEntity.getPackedBy() != null
                                ? packageEntity.getPackedBy().getName()
                                : null
                )

                .packedAt(
                        packageEntity.getPackedAt()
                )

                .sealedAt(
                        packageEntity.getSealedAt()
                )

                .items(
                        safeItems
                                .stream()
                                .map(this::toItemResponse)
                                .toList()
                )

                .build();
    }
}