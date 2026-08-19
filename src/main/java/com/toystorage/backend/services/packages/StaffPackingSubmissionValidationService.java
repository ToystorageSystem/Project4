package com.toystorage.backend.services.packages;

import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffPackingSubmissionValidationService {

    public void validateWarehouse(
            Users staff,
            StockTransfer transfer
    ) {

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }


        if (!staff
                .getWarehouse()
                .getId()
                .equals(
                        transfer
                                .getFromWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "Transfer belongs to another warehouse"
            );
        }
    }


    public void validateTransferStatus(
            StockTransfer transfer
    ) {

        if (transfer.getStatus()
                != TransferStatus.PACKING) {

            throw new BadRequest(
                    "Only PACKING transfer can be submitted"
            );
        }
    }


    public void validateItemsPacked(
            List<StockTransferItems> items
    ) {

        if (items.isEmpty()) {

            throw new BadRequest(
                    "Transfer contains no items"
            );
        }


        for (StockTransferItems item : items) {

            int picked =
                    item.getPickedQuantity() == null
                            ? 0
                            : item.getPickedQuantity();


            int packed =
                    item.getPackedQuantity() == null
                            ? 0
                            : item.getPackedQuantity();


            if (packed != picked) {

                throw new BadRequest(
                        "Product "
                                + item.getProduct().getName()
                                + " is not fully packed. "
                                + "Picked: "
                                + picked
                                + ", packed: "
                                + packed
                );
            }
        }
    }


    public void validatePackages(
            List<PackageTransferItem> relations
    ) {

        if (relations.isEmpty()) {

            throw new BadRequest(
                    "Transfer has no packages"
            );
        }


        for (PackageTransferItem relation : relations) {

            Packages packageEntity =
                    relation.getPackageEntity();


            if (packageEntity.getStatus()
                    != PackageStatus.PACKED) {

                throw new BadRequest(
                        "Package "
                                + packageEntity.getPackagesCode()
                                + " is not PACKED"
                );
            }


            if (packageEntity.getSealNumber() == null
                    || packageEntity
                    .getSealNumber()
                    .isBlank()) {

                throw new BadRequest(
                        "Package "
                                + packageEntity.getPackagesCode()
                                + " does not have seal number"
                );
            }


            if (packageEntity.getPackagesCode() == null
                    || packageEntity
                    .getPackagesCode()
                    .isBlank()) {

                throw new BadRequest(
                        "Package code is missing"
                );
            }
        }
    }
}