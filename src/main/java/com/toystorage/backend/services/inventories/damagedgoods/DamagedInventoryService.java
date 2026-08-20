package com.toystorage.backend.services.inventories.damagedgoods;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DamagedInventoryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;


    // =====================================================
    // LOẠI HÀNG HỎNG KHỎI AVAILABLE
    // =====================================================

    @Transactional
    public void removeFromAvailable(
            DamagedGoodsItems item,
            Integer quantity,
            Users manager
    ) {

        InventoryBalances balance =
                inventoryBalanceRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                item.getDamagedGoodsReport()
                                        .getWarehouse()
                                        .getId(),

                                item.getLocation().getId(),

                                item.getProduct().getId()
                        )

                        .orElseThrow(() ->
                                new NotFound(
                                        "Inventory balance not found"
                                )
                        );

        if (balance.getQuantity() < quantity) {

            throw new BadRequest(
                    "Damaged quantity exceeds physical inventory"
            );
        }

        if (balance.getAvailableQuantity() < quantity) {

            /*
             * Nếu quantity đó đã không còn available
             * thì không được trừ âm.
             */
            throw new BadRequest(
                    "Damaged quantity exceeds available inventory"
            );
        }

        int before =
                balance.getQuantity();

        /*
         * Hàng vẫn còn vật lý trong kho,
         * chỉ không còn khả dụng.
         */
        balance.setAvailableQuantity(
                balance.getAvailableQuantity()
                        - quantity
        );

        balance.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(balance);


        /*
         * Transaction DAMAGED.
         *
         * quantity tổng chưa giảm nếu chỉ quarantine.
         */
        InventoryTransactions transaction =
                new InventoryTransactions();

        transaction.setInventoryTransactionsCode(
                generateCode("IT")
        );

        transaction.setWarehouse(
                item.getDamagedGoodsReport()
                        .getWarehouse()
        );

        transaction.setLocation(
                item.getLocation()
        );

        transaction.setProduct(
                item.getProduct()
        );

        transaction.setQuantityBefore(before);

        transaction.setQuantityChange(0);

        transaction.setQuantityAfter(before);

        transaction.setTransactionType(
                InventoryTransactionType.DAMAGED
        );

        transaction.setReferenceType(
                InventoryReferenceType.INVENTORY_ADJUSTMENT
        );

        transaction.setReferenceId(
                item.getDamagedGoodsReport().getId()
        );

        transaction.setPerformedBy(manager);

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        inventoryTransactionRepository.save(
                transaction
        );
    }


    // =====================================================
    // TIÊU HỦY
    // =====================================================

    @Transactional
    public void dispose(
            DamagedGoodsItems item,
            Integer quantity,
            Users manager
    ) {

        InventoryBalances balance =
                inventoryBalanceRepository
                        .findByWarehouseIdAndLocationIdAndProductId(
                                item.getDamagedGoodsReport()
                                        .getWarehouse()
                                        .getId(),

                                item.getLocation().getId(),

                                item.getProduct().getId()
                        )

                        .orElseThrow(() ->
                                new NotFound(
                                        "Inventory balance not found"
                                )
                        );

        if (balance.getQuantity() < quantity) {

            throw new BadRequest(
                    "Dispose quantity exceeds inventory"
            );
        }

        int before =
                balance.getQuantity();

        int after =
                before - quantity;

        balance.setQuantity(after);

        /*
         * available đã được loại từ lúc xác định damaged.
         * Không trừ available lần thứ hai.
         */

        balance.setUpdatedAt(
                LocalDateTime.now()
        );

        inventoryBalanceRepository.save(balance);


        InventoryTransactions transaction =
                new InventoryTransactions();

        transaction.setInventoryTransactionsCode(
                generateCode("IT")
        );

        transaction.setWarehouse(
                item.getDamagedGoodsReport().getWarehouse()
        );

        transaction.setLocation(
                item.getLocation()
        );

        transaction.setProduct(
                item.getProduct()
        );

        transaction.setQuantityBefore(before);

        transaction.setQuantityChange(
                -quantity
        );

        transaction.setQuantityAfter(after);

        transaction.setTransactionType(
                InventoryTransactionType.DAMAGED
        );

        transaction.setReferenceType(
                InventoryReferenceType.INVENTORY_ADJUSTMENT
        );

        transaction.setReferenceId(
                item.getDamagedGoodsReport().getId()
        );

        transaction.setPerformedBy(manager);

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        inventoryTransactionRepository.save(transaction);
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