package com.toystorage.backend.services.stores;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.stores.StoreReturnItems;
import com.toystorage.backend.entity.stores.StoreReturns;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreReturnInventoryService {

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final InventoryTransactionRepository
            inventoryTransactionRepository;


    // =====================================================
    // GOOD PRODUCT -> AVAILABLE
    // =====================================================

    @Transactional
    public void addAvailableInventory(
            StoreReturns storeReturn,
            StoreReturnItems item,
            WarehouseLocations location,
            Integer quantity,
            Users manager
    ) {

        InventoryBalances balance =
                inventoryBalanceRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                storeReturn
                                        .getWarehouse()
                                        .getId(),

                                location.getId(),

                                item.getProduct()
                                        .getId()
                        )

                        .orElseGet(() -> {

                            InventoryBalances newBalance =
                                    new InventoryBalances();

                            newBalance.setInventoryBalancesCode(
                                    generateCode("IB")
                            );

                            newBalance.setWarehouse(
                                    storeReturn.getWarehouse()
                            );

                            newBalance.setLocation(
                                    location
                            );

                            newBalance.setProduct(
                                    item.getProduct()
                            );

                            newBalance.setQuantity(0);

                            newBalance.setAvailableQuantity(0);

                            newBalance.setReservedQuantity(0);

                            return newBalance;
                        });

        int before =
                balance.getQuantity();

        int availableBefore =
                balance.getAvailableQuantity();

        balance.setQuantity(
                before + quantity
        );

        /*
         * RULE:
         * Chỉ hàng NORMAL mới tăng available.
         */
        balance.setAvailableQuantity(
                availableBefore + quantity
        );

        balance.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(
                balance
        );

        createTransaction(
                storeReturn,
                item,
                location,
                before,
                quantity,
                before + quantity,
                manager
        );
    }


    // =====================================================
    // DAMAGED/QUARANTINE -> PHYSICAL ONLY
    // =====================================================

    @Transactional
    public void addQuarantineInventory(
            StoreReturns storeReturn,
            StoreReturnItems item,
            WarehouseLocations location,
            Integer quantity,
            Users manager
    ) {

        InventoryBalances balance =
                inventoryBalanceRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                storeReturn
                                        .getWarehouse()
                                        .getId(),

                                location.getId(),

                                item.getProduct()
                                        .getId()
                        )

                        .orElseGet(() -> {

                            InventoryBalances newBalance =
                                    new InventoryBalances();

                            newBalance.setInventoryBalancesCode(
                                    generateCode("IB")
                            );

                            newBalance.setWarehouse(
                                    storeReturn.getWarehouse()
                            );

                            newBalance.setLocation(
                                    location
                            );

                            newBalance.setProduct(
                                    item.getProduct()
                            );

                            newBalance.setQuantity(0);

                            newBalance.setAvailableQuantity(0);

                            newBalance.setReservedQuantity(0);

                            return newBalance;
                        });

        int before =
                balance.getQuantity();

        balance.setQuantity(
                before + quantity
        );

        /*
         * TUYỆT ĐỐI KHÔNG:
         *
         * balance.setAvailableQuantity(...)
         *
         * vì đây là hàng quarantine/damaged.
         */

        balance.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(
                balance
        );

        createTransaction(
                storeReturn,
                item,
                location,
                before,
                quantity,
                before + quantity,
                manager
        );
    }


    private void createTransaction(
            StoreReturns storeReturn,
            StoreReturnItems item,
            WarehouseLocations location,
            int before,
            int change,
            int after,
            Users manager
    ) {

        InventoryTransactions transaction =
                new InventoryTransactions();

        transaction.setInventoryTransactionsCode(
                generateCode("IT")
        );

        transaction.setWarehouse(
                storeReturn.getWarehouse()
        );

        transaction.setLocation(
                location
        );

        transaction.setProduct(
                item.getProduct()
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

        /*
         * Schema inventory transaction của bạn
         * đã hỗ trợ STORE_RETURN.
         */
        transaction.setReferenceType(
                InventoryReferenceType.STORE_RETURN
        );

        transaction.setReferenceId(
                storeReturn.getId()
        );

        transaction.setTransactionType(
                InventoryTransactionType.STORE_RETURN_IN
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