package com.toystorage.backend.services.shipments;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.transfers.picking.StockTransferItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * Goods physically leave the source warehouse when the shipment is confirmed (2026-11-13):
 * - stock_transfer_items.shipped_quantity = packed_quantity (the store uses it as expected quantity),
 * - the reservation made at submit is consumed: reserved -= shipped AND quantity -= shipped.
 *
 * No new inventory transaction is written: the TRANSFER_OUT rows created at submit already record
 * this outflow for the transfer (writing another one would double-count it in reports).
 */
@Service
@RequiredArgsConstructor
public class TransferShipmentInventoryService {

    private final StockTransferItemRepository stockTransferItemRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void shipFromSource(StockTransfer transfer) {
        Long warehouseId = transfer.getFromWarehouse().getId();
        List<StockTransferItems> items = stockTransferItemRepository.findByStockTransferId(transfer.getId());
        if (items.isEmpty()) {
            throw new BadRequest("Stock transfer contains no items");
        }

        // stable order: product id (same order as the reservation, avoids deadlocks)
        items = items.stream()
                .sorted((a, b) -> a.getProduct().getId().compareTo(b.getProduct().getId()))
                .toList();

        for (StockTransferItems item : items) {
            int shipped = item.getPackedQuantity() == null ? 0 : item.getPackedQuantity();
            item.setShippedQuantity(shipped);
            if (shipped > 0) {
                deduct(warehouseId, item, shipped);
            }
        }
        stockTransferItemRepository.saveAll(items);
    }

    private void deduct(Long warehouseId, StockTransferItems item, int shipped) {
        Long productId = item.getProduct().getId();
        List<InventoryBalances> balances =
                inventoryBalanceRepository.findByWarehouseIdAndProductIdForUpdate(warehouseId, productId);

        int remaining = shipped;

        // 1. consume the reservation made when the transfer was submitted
        for (InventoryBalances balance : balances) {
            if (remaining == 0) break;
            int reserved = orZero(balance.getReservedQuantity());
            int quantity = orZero(balance.getQuantity());
            int take = Math.min(remaining, Math.min(reserved, quantity));
            if (take <= 0) continue;
            balance.setReservedQuantity(reserved - take);
            balance.setQuantity(quantity - take);
            remaining -= take;
        }

        // 2. old data without a reservation: take from free stock
        for (InventoryBalances balance : balances) {
            if (remaining == 0) break;
            int quantity = orZero(balance.getQuantity());
            // available_quantity is 0 for RECEIVING / DAMAGED / QUARANTINE locations
            int free = orZero(balance.getAvailableQuantity());
            int take = Math.min(remaining, free);
            if (take <= 0) continue;
            balance.setQuantity(quantity - take);
            remaining -= take;
        }

        if (remaining > 0) {
            throw new BadRequest("Not enough inventory to ship product "
                    + item.getProduct().getName() + ". Missing: " + remaining);
        }
        inventoryBalanceRepository.saveAll(balances);
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
