package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.PutawayTaskItems;
import com.toystorage.backend.entity.warehouses.PutawayTasks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;

import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;

import com.toystorage.backend.exceptions.BadRequest;

import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PutawayInventoryService {

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final InventoryTransactionRepository
            inventoryTransactionRepository;


    @Transactional
    public void moveInventory(
            PutawayTasks task,
            PutawayTaskItems item,
            Users staff,
            int quantity
    ) {

        Long warehouseId =
                task.getWarehouse().getId();

        Long productId =
                item.getProduct().getId();

        WarehouseLocations from =
                item.getFromLocation();

        WarehouseLocations to =
                item.getToLocation();


        if (from == null || to == null) {
            throw new BadRequest("Putaway source or destination location is missing. Contact warehouse manager.");
        }

        if (quantity <= 0) {
            throw new BadRequest("Putaway quantity must be greater than 0");
        }
        if (from.getId().equals(to.getId())) {
            throw new BadRequest("Putaway source and destination must be different");
        }

        // =================================================
        // SOURCE BALANCE
        // =================================================

        InventoryBalances source =
                inventoryBalanceRepository
                        .findBalanceForUpdate(
                                warehouseId,
                                from.getId(),
                                productId
                        )
                        .orElseThrow(() ->
                                new BadRequest(
                                        "Inventory not found "
                                                + "at source location"
                                )
                        );


        if ((long) source.getQuantity() - source.getReservedQuantity() < quantity) {

            throw new BadRequest(
                    "Source location does not have "
                            + "enough inventory"
            );
        }


        int sourceBefore =
                source.getQuantity();

        int sourceAfter =
                sourceBefore - quantity;

        source.setQuantity(
                sourceAfter
        );

        /*
         * Receiving stock trước putaway
         * chưa được coi là available.
         */
        source.setAvailableQuantity(
                Math.min(
                        source.getAvailableQuantity(),
                        sourceAfter
                )
        );

        source.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(
                source
        );


        // =================================================
        // DESTINATION BALANCE
        // =================================================

        InventoryBalances destination =
                inventoryBalanceRepository
                        .findBalanceForUpdate(
                                warehouseId,
                                to.getId(),
                                productId
                        )

                        .orElseGet(() -> {

                            InventoryBalances balance =
                                    new InventoryBalances();

                            balance.setInventoryBalancesCode(
                                    generateCode("IB")
                            );

                            balance.setWarehouse(
                                    task.getWarehouse()
                            );

                            balance.setLocation(to);

                            balance.setProduct(
                                    item.getProduct()
                            );

                            balance.setQuantity(0);

                            balance.setAvailableQuantity(0);

                            balance.setReservedQuantity(0);

                            return balance;
                        });


        int destinationBefore =
                destination.getQuantity();

        int destinationAfter =
                Math.addExact(destinationBefore, quantity);

        destination.setQuantity(
                destinationAfter
        );

        /*
         * Sau putaway vào NORMAL location,
         * hàng mới trở thành available.
         */
        destination.setAvailableQuantity(
                destination.getAvailableQuantity()
        );

        destination.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(
                destination
        );


        createTransaction(task, item, staff, from,
                sourceBefore, -quantity, sourceAfter);

        createTransaction(
                task,
                item,
                staff,
                to,
                destinationBefore,
                quantity,
                destinationAfter
        );
    }


    private void createTransaction(
            PutawayTasks task,
            PutawayTaskItems item,
            Users staff,
            WarehouseLocations location,
            int before,
            int change,
            int after
    ) {

        InventoryTransactions transaction =
                new InventoryTransactions();

        transaction.setInventoryTransactionsCode(
                generateCode("IT")
        );

        transaction.setWarehouse(
                task.getWarehouse()
        );

        transaction.setProduct(
                item.getProduct()
        );

        transaction.setLocation(
                location
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

        transaction.setTransactionType(
                InventoryTransactionType.LOCATION_TRANSFER
        );

        transaction.setReferenceType(
                InventoryReferenceType.LOCATION_TRANSFER
        );

        transaction.setReferenceId(
                task.getId()
        );

        transaction.setPerformedBy(
                staff
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