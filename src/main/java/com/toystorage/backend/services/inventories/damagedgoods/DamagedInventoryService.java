package com.toystorage.backend.services.inventories.damagedgoods;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;

import com.toystorage.backend.enums.warehouses.WarehouseLocationType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;

import com.toystorage.backend.services.warehouses.damagedgoods.DamagedGoodsLocationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class DamagedInventoryService {

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final InventoryTransactionRepository
            inventoryTransactionRepository;

    private final DamagedGoodsLocationService
            damagedGoodsLocationService;


    // =====================================================
    // MOVE TO QUARANTINE
    // =====================================================

    @Transactional
    public WarehouseLocations moveToQuarantine(
            DamagedGoodsItems item,
            Integer quantity,
            Users manager
    ) {

        Long warehouseId =
                item.getDamagedGoodsReport()
                        .getWarehouse()
                        .getId();


        WarehouseLocations sourceLocation =
                item.getLocation();


        WarehouseLocations quarantineLocation =
                damagedGoodsLocationService
                        .getQuarantineLocation(
                                warehouseId
                        );


        /*
         * Item đã nằm ở quarantine từ trước
         * ví dụ Store Return.
         */
        if (
                sourceLocation.getId()
                        .equals(
                                quarantineLocation.getId()
                        )

                        || sourceLocation.getLocationType()
                        == WarehouseLocationType.QUARANTINE

                        || sourceLocation.getLocationType()
                        == WarehouseLocationType.DAMAGED
        ) {

            InventoryBalances balance =
                    getBalance(
                            warehouseId,
                            sourceLocation.getId(),
                            item.getProduct().getId()
                    );


            if (balance.getQuantity() < quantity) {

                throw new BadRequest(
                        "Damaged quantity exceeds quarantine inventory"
                );
            }


            return sourceLocation;
        }


        InventoryBalances sourceBalance =
                getBalance(
                        warehouseId,
                        sourceLocation.getId(),
                        item.getProduct().getId()
                );


        /*
         * Không được lấy lượng hàng đang reserve.
         */
        if (
                sourceBalance.getAvailableQuantity()
                        < quantity
        ) {

            throw new BadRequest(
                    "Damaged quantity exceeds available inventory"
            );
        }


        int sourceBefore =
                sourceBalance.getQuantity();


        int sourceAfter =
                sourceBefore - quantity;


        sourceBalance.setQuantity(
                sourceAfter
        );


        inventoryBalanceRepository.save(
                sourceBalance
        );


        InventoryBalances quarantineBalance =
                inventoryBalanceRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                warehouseId,
                                quarantineLocation.getId(),
                                item.getProduct().getId()
                        )

                        .orElseGet(() -> {

                            InventoryBalances balance =
                                    new InventoryBalances();

                            balance.setInventoryBalancesCode(
                                    generateCode("IB")
                            );

                            balance.setWarehouse(
                                    item
                                            .getDamagedGoodsReport()
                                            .getWarehouse()
                            );

                            balance.setLocation(
                                    quarantineLocation
                            );

                            balance.setProduct(
                                    item.getProduct()
                            );

                            balance.setQuantity(0);

                            balance.setReservedQuantity(0);

                            return balance;
                        });


        int quarantineBefore =
                quarantineBalance.getQuantity() == null
                        ? 0
                        : quarantineBalance.getQuantity();


        int quarantineAfter =
                quarantineBefore + quantity;


        quarantineBalance.setQuantity(
                quarantineAfter
        );


        inventoryBalanceRepository.save(
                quarantineBalance
        );


        createTransaction(
                item,
                sourceLocation,
                InventoryTransactionType.LOCATION_TRANSFER,
                -quantity,
                sourceBefore,
                sourceAfter,
                manager
        );


        createTransaction(
                item,
                quarantineLocation,
                InventoryTransactionType.LOCATION_TRANSFER,
                quantity,
                quarantineBefore,
                quarantineAfter,
                manager
        );


        return quarantineLocation;
    }


    // =====================================================
    // DISPOSE
    // =====================================================

    @Transactional
    public void dispose(
            DamagedGoodsItems item,
            Integer quantity,
            Users manager
    ) {

        InventoryBalances balance =
                getBalance(
                        item.getDamagedGoodsReport()
                                .getWarehouse()
                                .getId(),

                        item.getLocation().getId(),

                        item.getProduct().getId()
                );


        if (balance.getQuantity() < quantity) {

            throw new BadRequest(
                    "Dispose quantity exceeds quarantine inventory"
            );
        }


        int before =
                balance.getQuantity();


        int after =
                before - quantity;


        balance.setQuantity(
                after
        );


        inventoryBalanceRepository.save(
                balance
        );


        createTransaction(
                item,
                item.getLocation(),
                InventoryTransactionType.DAMAGED,
                -quantity,
                before,
                after,
                manager
        );
    }


    // =====================================================
    // RETURN SUPPLIER
    // =====================================================

    @Transactional
    public void returnToSupplier(
            DamagedGoodsItems item,
            Integer quantity,
            Users manager
    ) {

        InventoryBalances balance =
                getBalance(
                        item.getDamagedGoodsReport()
                                .getWarehouse()
                                .getId(),

                        item.getLocation().getId(),

                        item.getProduct().getId()
                );


        if (balance.getQuantity() < quantity) {

            throw new BadRequest(
                    "Return quantity exceeds quarantine inventory"
            );
        }


        int before =
                balance.getQuantity();


        int after =
                before - quantity;


        balance.setQuantity(
                after
        );


        inventoryBalanceRepository.save(
                balance
        );


        createTransaction(
                item,
                item.getLocation(),
                InventoryTransactionType
                        .DAMAGED_RETURN_TO_SUPPLIER,
                -quantity,
                before,
                after,
                manager
        );
    }


    // =====================================================
    // GET BALANCE
    // =====================================================

    private InventoryBalances getBalance(
            Long warehouseId,
            Long locationId,
            Long productId
    ) {

        return inventoryBalanceRepository
                .findByWarehouseIdAndLocationIdAndProductId(
                        warehouseId,
                        locationId,
                        productId
                )

                .orElseThrow(() ->
                        new NotFound(
                                "Inventory balance not found"
                        )
                );
    }


    // =====================================================
    // TRANSACTION
    // =====================================================

    private void createTransaction(
            DamagedGoodsItems item,
            WarehouseLocations location,
            InventoryTransactionType transactionType,
            Integer change,
            Integer before,
            Integer after,
            Users manager
    ) {

        InventoryTransactions transaction =
                new InventoryTransactions();


        transaction.setInventoryTransactionsCode(
                generateCode("IT")
        );


        transaction.setWarehouse(
                item
                        .getDamagedGoodsReport()
                        .getWarehouse()
        );


        transaction.setLocation(
                location
        );


        transaction.setProduct(
                item.getProduct()
        );


        transaction.setTransactionType(
                transactionType
        );


        transaction.setQuantityBefore(
                before
        );


        transaction.setQuantityChange(
                change
        );


        transaction.setQuantityAfter(
                after
        );


        transaction.setReferenceType(
                InventoryReferenceType.DAMAGED_GOODS
        );


        transaction.setReferenceId(
                item
                        .getDamagedGoodsReport()
                        .getId()
        );


        transaction.setPerformedBy(
                manager
        );


        transaction.setCreatedAt(
                LocalDateTime.now()
        );


        inventoryTransactionRepository.save(
                transaction
        );
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