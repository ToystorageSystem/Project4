package com.toystorage.backend.services.inventories.damagedlot;

import com.toystorage.backend.entity.inventories.DamagedStockLot;
import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.inventories.InventoryTransactions;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import com.toystorage.backend.repository.inventories.InventoryTransactionRepository;
import com.toystorage.backend.repository.inventories.damagedlot.DamagedStockLotRepository;
import com.toystorage.backend.repository.warehouses.putaway.WarehouseLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dua hang hong / loi vao khu hang hong theo lo. Hang o khu QUARANTINE / DAMAGED khong tinh vao ton kha dung
 * (InventoryBalances.availableQuantity = 0 cho cac loai vi tri nay).
 */
@Service
@RequiredArgsConstructor
public class DamagedStockService {

    private final InventoryBalanceRepository balances;
    private final InventoryTransactionRepository transactions;
    private final WarehouseLocationRepository locations;
    private final DamagedStockLotRepository lots;

    /** Khu hang hong cua kho: uu tien QUARANTINE, neu khong co thi DAMAGED. */
    @Transactional(readOnly = true)
    public WarehouseLocations damagedArea(Long warehouseId) {
        return locations.findFirstByWarehouseIdAndLocationTypeAndStatus(warehouseId, WarehouseLocationType.QUARANTINE, WarehouseStatus.ACTIVE)
                .or(() -> locations.findFirstByWarehouseIdAndLocationTypeAndStatus(warehouseId, WarehouseLocationType.DAMAGED, WarehouseStatus.ACTIVE))
                .orElseThrow(() -> new BadRequest(
                        "Create an active QUARANTINE or DAMAGED location in Storage locations before recording damaged goods"));
    }

    /**
     * Cong so luong hang hong vao khu hang hong va ghi lo.
     * referenceType / transactionType dung lai loai cua chung tu nguon de giu lich su ton kho.
     */
    @Transactional
    public DamagedStockLot record(Warehouses warehouse, Products product, int quantity, String sourceType, Long sourceId,
                                  String sourceCode, String note, Users actor,
                                  InventoryReferenceType referenceType, InventoryTransactionType transactionType) {
        return record(warehouse, product, quantity, sourceType, sourceId, sourceCode,
                "LOT-" + (sourceCode == null ? sourceType + "-" + sourceId : sourceCode),
                note, actor, referenceType, transactionType);
    }

    /** Ghi lo voi ma lo chi dinh (vd hang loi tu nha cung cap: ma lo = ma don PO). */
    @Transactional
    public DamagedStockLot record(Warehouses warehouse, Products product, int quantity, String sourceType, Long sourceId,
                                  String sourceCode, String lotCode, String note, Users actor,
                                  InventoryReferenceType referenceType, InventoryTransactionType transactionType) {
        if (quantity <= 0) return null;
        WarehouseLocations area = damagedArea(warehouse.getId());
        InventoryBalances balance = balances.findBalanceForUpdate(warehouse.getId(), area.getId(), product.getId())
                .orElseGet(() -> {
                    InventoryBalances b = new InventoryBalances();
                    b.setInventoryBalancesCode(code("IB"));
                    b.setWarehouse(warehouse);
                    b.setLocation(area);
                    b.setProduct(product);
                    b.setQuantity(0);
                    b.setReservedQuantity(0);
                    b.setAvailableQuantity(0);
                    b.setUpdatedAt(LocalDateTime.now());
                    return b;
                });
        int before = balance.getQuantity() == null ? 0 : balance.getQuantity();
        int after = Math.addExact(before, quantity);
        balance.setQuantity(after);
        balance.setUpdatedAt(LocalDateTime.now());
        balances.save(balance);

        InventoryTransactions t = new InventoryTransactions();
        t.setInventoryTransactionsCode(code("IT"));
        t.setProduct(product);
        t.setWarehouse(warehouse);
        t.setLocation(area);
        t.setQuantityBefore(before);
        t.setQuantityChange(quantity);
        t.setQuantityAfter(after);
        t.setReferenceId(sourceId);
        t.setReferenceType(referenceType);
        t.setTransactionType(transactionType);
        t.setPerformedBy(actor);
        t.setCreatedAt(LocalDateTime.now());
        transactions.save(t);

        return lots.save(DamagedStockLot.builder()
                .lotCode(lotCode)
                .sourceType(sourceType).sourceId(sourceId).sourceCode(sourceCode)
                .warehouse(warehouse).location(area).product(product).quantity(quantity)
                .note(note == null ? null : note.length() > 500 ? note.substring(0, 500) : note)
                .recordedBy(actor).build());
    }

    private static String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
