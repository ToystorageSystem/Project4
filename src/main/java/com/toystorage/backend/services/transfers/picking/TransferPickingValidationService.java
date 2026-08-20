package com.toystorage.backend.services.transfers.picking;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.transfers.TransferStatus;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.transfers.StockTransferItemRepository;
import com.toystorage.backend.repository.transfers.StockTransferRepository;
import com.toystorage.backend.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferPickingValidationService {

    private final UserRepository
            userRepository;

    private final StockTransferRepository
            stockTransferRepository;

    private final StockTransferItemRepository
            stockTransferItemRepository;

    private final InventoryBalanceRepository
            inventoryBalanceRepository;


    // =====================================================
    // CURRENT USER
    // =====================================================

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


    // =====================================================
    // TRANSFER
    // =====================================================

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


    // =====================================================
    // ITEM
    // =====================================================

    public StockTransferItems getItem(
            Long transferId,
            Long itemId
    ) {

        return stockTransferItemRepository
                .findByIdAndStockTransferId(
                        itemId,
                        transferId
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Transfer item not found"
                        )
                );
    }


    // =====================================================
    // WAREHOUSE
    // =====================================================

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
                        transfer.getFromWarehouse().getId()
                )) {

            throw new Forbidden(
                    "Transfer belongs to another warehouse"
            );
        }
    }


    // =====================================================
    // PICKING STATUS
    // =====================================================

    public void validatePickingStatus(
            StockTransfer transfer
    ) {

        if (transfer.getStatus()
                != TransferStatus.PICKING) {

            throw new BadRequest(
                    "Stock transfer is not in PICKING status"
            );
        }
    }


    // =====================================================
    // QUANTITY
    // =====================================================

    public void validateQuantity(
            StockTransferItems item,
            Integer quantity
    ) {

        if (quantity == null
                || quantity <= 0) {

            throw new BadRequest(
                    "Picked quantity must be greater than 0"
            );
        }

        int currentPicked =
                item.getPickedQuantity() == null
                        ? 0
                        : item.getPickedQuantity();

        int after =
                currentPicked
                        + quantity;

        if (after
                > item.getApprovedQuantity()) {

            throw new BadRequest(
                    "Picked quantity cannot exceed approved quantity"
            );
        }
    }


    // =====================================================
    // BARCODE + LOCATION
    // =====================================================

    public InventoryBalances validateScanAndLocation(
            StockTransfer transfer,
            StockTransferItems item,
            String productBarcode,
            String locationCode
    ) {

        if (productBarcode == null
                || !item.getProduct()
                .getBarcode()
                .equals(productBarcode)) {

            throw new BadRequest(
                    "Scanned product barcode does not match product"
            );
        }


        InventoryBalances balance =
                inventoryBalanceRepository
                        .findPickingBalance(
                                transfer
                                        .getFromWarehouse()
                                        .getId(),

                                item.getProduct()
                                        .getId(),

                                locationCode
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Product is not stored "
                                                + "at scanned location"
                                )
                        );


        /*
         * Không giảm balance ở bước picking.
         *
         * Transfer đã reserve / deduct
         * ở bước confirmation.
         */

        return balance;
    }
}