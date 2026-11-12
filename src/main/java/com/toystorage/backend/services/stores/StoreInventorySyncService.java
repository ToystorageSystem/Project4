package com.toystorage.backend.services.stores;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Store stock is kept in store_stocks (backroom + shelf). The team's reports, low-stock alerts and
 * transfer recommendations read inventory_balances, so after every store stock change the TOTAL
 * (shelf + backroom) is copied to inventory_balances of the store (decision 2026-11-13, "cách 2").
 *
 * One balance row per (store, product) holds the total; other rows of the same product are set to 0.
 * If the store has no NORMAL location yet, the sync is skipped (logged) instead of failing the sale/receipt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreInventorySyncService {

    private final EntityManager entityManager;

    /** Call after the StoreStocks row was changed, inside the same transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sync(StoreStocks stock) {
        sync(stock.getStoreId(), stock.getProduct(), stock.getShelfQuantity() + stock.getBackroomQuantity());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void sync(Long storeId, Products product, int total) {
        List<InventoryBalances> balances = entityManager.createQuery("""
                SELECT b FROM InventoryBalances b
                WHERE b.warehouse.id = :storeId AND b.product.id = :productId
                ORDER BY b.id
                """, InventoryBalances.class)
                .setParameter("storeId", storeId)
                .setParameter("productId", product.getId())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        LocalDateTime now = LocalDateTime.now();
        if (balances.isEmpty()) {
            WarehouseLocations location = firstNormalLocation(storeId);
            if (location == null) {
                log.warn("Store {} has no NORMAL location: inventory_balances not synced for product {}",
                        storeId, product.getId());
                return;
            }
            InventoryBalances balance = new InventoryBalances();
            balance.setInventoryBalancesCode("IB-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase());
            balance.setWarehouse(entityManager.getReference(Warehouses.class, storeId));
            balance.setLocation(location);
            balance.setProduct(product);
            balance.setQuantity(total);
            balance.setReservedQuantity(0);
            balance.setAvailableQuantity(total);
            balance.setUpdatedAt(now);
            entityManager.persist(balance);
            return;
        }

        for (int i = 0; i < balances.size(); i++) {
            InventoryBalances balance = balances.get(i);
            int quantity = i == 0 ? total : 0;
            int reserved = balance.getReservedQuantity() == null ? 0 : balance.getReservedQuantity();
            balance.setQuantity(quantity);
            balance.setReservedQuantity(Math.min(reserved, quantity));
            balance.setUpdatedAt(now);                 // available is recalculated by @PreUpdate
        }
    }

    private WarehouseLocations firstNormalLocation(Long storeId) {
        return entityManager.createQuery("""
                SELECT l FROM WarehouseLocations l
                WHERE l.warehouse.id = :storeId AND l.locationType = :type
                ORDER BY l.id
                """, WarehouseLocations.class)
                .setParameter("storeId", storeId)
                .setParameter("type", WarehouseLocationType.NORMAL)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
