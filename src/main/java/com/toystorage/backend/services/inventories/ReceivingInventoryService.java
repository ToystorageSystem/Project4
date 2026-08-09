package com.toystorage.backend.services.inventories;

import com.toystorage.backend.entity.inventory.InventoryBalances;
import com.toystorage.backend.entity.inventory.InventoryTransactions;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.inventory.InventoryReferenceType;
import com.toystorage.backend.enums.inventory.InventoryTransactionType;
import com.toystorage.backend.repository.inventory.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventory.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceivingInventoryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public void updateReceivingInventory(
            GoodsReceipts receipt,
            List<GoodsReceiptItems> items,
            Users manager,
            WarehouseLocations receivingLocation
    ) {

        for (GoodsReceiptItems item : items) {

            Integer acceptedQuantity =
                    item.getAcceptedQuantity();

            if (acceptedQuantity == null
                    || acceptedQuantity <= 0) {
                continue;
            }

            updateInventoryItem(
                    receipt,
                    item,
                    manager,
                    receivingLocation
            );
        }
    }

    private void updateInventoryItem(
            GoodsReceipts receipt,
            GoodsReceiptItems item,
            Users manager,
            WarehouseLocations location
    ) {

        Long warehouseId =
                receipt.getWarehouse().getId();

        Long locationId =
                location.getId();

        Long productId =
                item.getProduct().getId();

        InventoryBalances balance =
                inventoryBalanceRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                warehouseId,
                                locationId,
                                productId
                        )
                        .orElseGet(() -> {

                            InventoryBalances newBalance =
                                    new InventoryBalances();

                            newBalance.setInventoryBalancesCode(
                                    generateCode("IB")
                            );

                            newBalance.setWarehouse(
                                    receipt.getWarehouse()
                            );

                            newBalance.setLocation(location);

                            newBalance.setProduct(
                                    item.getProduct()
                            );

                            newBalance.setQuantity(0);
                            newBalance.setAvailableQuantity(0);
                            newBalance.setReservedQuantity(0);

                            newBalance.setUpdatedAt(
                                    LocalDateTime.now()
                            );

                            return newBalance;
                        });

        int before = balance.getQuantity();

        int accepted =
                item.getAcceptedQuantity();

        int after =
                before + accepted;

        /*
         * Hàng mới nhận chỉ nằm ở receiving location.
         *
         * Chưa tăng availableQuantity.
         * Sau Putaway mới trở thành available.
         */
        balance.setQuantity(after);

        balance.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(balance);

        InventoryTransactions transaction =
                new InventoryTransactions();

        transaction.setInventoryTransactionsCode(
                generateCode("IT")
        );

        transaction.setProduct(
                item.getProduct()
        );

        transaction.setWarehouse(
                receipt.getWarehouse()
        );

        transaction.setLocation(location);

        transaction.setQuantityBefore(before);

        transaction.setQuantityChange(
                accepted
        );

        transaction.setQuantityAfter(after);

        transaction.setReferenceId(
                receipt.getId()
        );

        transaction.setReferenceType(
                InventoryReferenceType.GOODS_RECEIPT
        );

        transaction.setTransactionType(
                InventoryTransactionType.GOODS_RECEIPT
        );

        transaction.setPerformedBy(manager);

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        inventoryTransactionRepository.save(
                transaction
        );
    }

    private String generateCode(String prefix) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}