package com.toystorage.backend.services.packages;

import com.toystorage.backend.entity.packages.Packages;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.packages.PackageStatus;
import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.packages.StaffPackageRepository;

import com.toystorage.backend.repository.transfers.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.StockTransferRepository;

import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffPackagePackingValidationService {

    private final UserRepository userRepository;

    private final StaffPackageRepository packageRepository;

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;


    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    public StockTransfer getTransfer(
            Long transferId
    ) {

        return stockTransferRepository
                .findById(transferId)
                .orElseThrow(() ->
                        new NotFound(
                                "Stock transfer not found"
                        )
                );
    }


    public StockTransferItems getTransferItem(
            Long transferId,
            String barcode
    ) {

        return stockTransferItemRepository
                .findByTransferAndBarcode(
                        transferId,
                        barcode
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Product does not belong to transfer"
                        )
                );
    }


    public Packages getPackage(
            Long packageId
    ) {

        return packageRepository
                .findById(
                        packageId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Package not found"
                        )
                );
    }


    public void validateWarehouse(
            Users staff,
            StockTransfer transfer
    ) {

        if (staff.getWarehouse() == null) {

            throw new Forbidden(
                    "User is not assigned to warehouse"
            );
        }

        if (!staff.getWarehouse()
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


    public void validateTransferPacking(
            StockTransfer transfer
    ) {

        if (transfer.getStatus()
                != TransferStatus.PACKING) {

            throw new BadRequest(
                    "Transfer must be in PACKING status"
            );
        }
    }


    public void validatePackageEditable(
            Packages packageEntity
    ) {

        if (packageEntity.getStatus()
                != PackageStatus.CREATED

                &&

                packageEntity.getStatus()
                        != PackageStatus.PACKING) {

            throw new BadRequest(
                    "Package is no longer editable"
            );
        }
    }


    public void validatePackedQuantity(
            StockTransferItems item,
            int alreadyPacked,
            int adding
    ) {

        int picked =
                item.getPickedQuantity() == null
                        ? 0
                        : item.getPickedQuantity();

        if (alreadyPacked + adding > picked) {

            throw new BadRequest(
                    "Packed quantity exceeds picked quantity. "
                            + "Remaining: "
                            + Math.max(
                            picked - alreadyPacked,
                            0
                    )
            );
        }
    }
}