package com.toystorage.backend.services.packages.packing;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.packages.PackageItems;
import com.toystorage.backend.entity.packages.PackageTransferItem;
import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.transfers.StockTransferRepository;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.packages.PackageItemRepository;
import com.toystorage.backend.repository.packages.PackageTransferItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackingValidationService {

    private final UserRepository userRepository;

    private final StockTransferRepository
            stockTransferRepository;

    private final PackageTransferItemRepository
            packageTransferItemRepository;

    private final PackageItemRepository
            packageItemRepository;


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
                                "Stock transfer not found: "
                                        + transferId
                        )
                );
    }


    public void validateWarehouse(
            Users manager,
            StockTransfer transfer
    ) {

        if (manager.getWarehouse() == null) {

            throw new Forbidden(
                    "Manager is not assigned to a warehouse"
            );
        }

        /*
         * Manager chỉ confirm hàng xuất
         * từ kho của chính mình.
         */
        if (!manager.getWarehouse()
                .getId()
                .equals(
                        transfer
                                .getFromWarehouse()
                                .getId()
                )) {

            throw new Forbidden(
                    "You cannot confirm packing "
                            + "for another warehouse"
            );
        }
    }


    public List<PackageTransferItem>
    getTransferPackages(
            Long transferId
    ) {

        List<PackageTransferItem> packages =
                packageTransferItemRepository
                        .findByStockTransferId(
                                transferId
                        );

        if (packages.isEmpty()) {

            throw new BadRequest(
                    "Stock transfer has no packages"
            );
        }

        return packages;
    }


    public void validatePackage(
            Packages pack
    ) {

        /*
         * Package phải được staff đóng xong.
         */
        if (!"PACKED".equals(
                pack.getStatus().name()
        )) {

            throw new BadRequest(
                    "Package "
                            + pack.getPackagesCode()
                            + " is not PACKED"
            );
        }

        /*
         * Kiểm tra niêm phong.
         *
         * Schema hiện tại chỉ có seal_number,
         * chưa có seal_status.
         */
        if (pack.getSealNumber() == null
                || pack.getSealNumber()
                .isBlank()) {

            throw new BadRequest(
                    "Package "
                            + pack.getPackagesCode()
                            + " has no seal number"
            );
        }

        List<PackageItems> items =
                packageItemRepository
                        .findByPackageEntityId(
                                pack.getId()
                        );

        if (items.isEmpty()) {

            throw new BadRequest(
                    "Package "
                            + pack.getPackagesCode()
                            + " contains no items"
            );
        }
    }
}