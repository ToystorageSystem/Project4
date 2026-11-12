package com.toystorage.backend.services.warehouses.putaway;

import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Tells the staff where to put a product away ("go to Zone A · Shelf 03 · A-03-02"):
 *  1. the shelf the manager assigned to this product (zone setup),
 *  2. a shelf that already holds the same product (keep stock together),
 *  3. an empty NORMAL shelf,
 *  4. any active NORMAL shelf.
 * It is a suggestion; the staff can still scan another NORMAL shelf if it is full.
 */
@Service
@RequiredArgsConstructor
public class PutawaySuggestionService {

    private final EntityManager entityManager;

    public record Suggestion(Long locationId, String locationCode, String zone, String shelf, String reason) {
    }

    @Transactional(readOnly = true)
    public Optional<Suggestion> suggest(Long warehouseId, Long productId) {
        if (warehouseId == null || productId == null) return Optional.empty();

        Optional<WarehouseLocations> assigned = first(entityManager.createQuery("""
                        SELECT lp.location FROM WarehouseLocationProduct lp
                        WHERE lp.product.id = :productId
                          AND lp.location.warehouse.id = :warehouseId
                          AND lp.location.locationType = :normal
                          AND lp.location.status = :active
                        ORDER BY lp.location.zone, lp.location.shelf, lp.location.warehouseCode
                        """, WarehouseLocations.class)
                .setParameter("productId", productId)
                .setParameter("warehouseId", warehouseId)
                .setParameter("normal", WarehouseLocationType.NORMAL)
                .setParameter("active", WarehouseStatus.ACTIVE)
                .setMaxResults(1)
                .getResultList());
        if (assigned.isPresent()) return assigned.map(l -> of(l, "Shelf assigned to this product"));

        Optional<WarehouseLocations> sameProduct = first(entityManager.createQuery("""
                        SELECT b.location FROM InventoryBalances b
                        WHERE b.product.id = :productId
                          AND b.warehouse.id = :warehouseId
                          AND b.quantity > 0
                          AND b.location.locationType = :normal
                          AND b.location.status = :active
                        ORDER BY b.quantity DESC
                        """, WarehouseLocations.class)
                .setParameter("productId", productId)
                .setParameter("warehouseId", warehouseId)
                .setParameter("normal", WarehouseLocationType.NORMAL)
                .setParameter("active", WarehouseStatus.ACTIVE)
                .setMaxResults(1)
                .getResultList());
        if (sameProduct.isPresent()) return sameProduct.map(l -> of(l, "Same product is already stored here"));

        Optional<WarehouseLocations> empty = first(entityManager.createQuery("""
                        SELECT l FROM WarehouseLocations l
                        WHERE l.warehouse.id = :warehouseId
                          AND l.locationType = :normal
                          AND l.status = :active
                          AND NOT EXISTS (
                              SELECT 1 FROM InventoryBalances b WHERE b.location = l AND b.quantity > 0)
                        ORDER BY l.zone, l.shelf, l.warehouseCode
                        """, WarehouseLocations.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("normal", WarehouseLocationType.NORMAL)
                .setParameter("active", WarehouseStatus.ACTIVE)
                .setMaxResults(1)
                .getResultList());
        if (empty.isPresent()) return empty.map(l -> of(l, "Empty shelf"));

        return first(entityManager.createQuery("""
                        SELECT l FROM WarehouseLocations l
                        WHERE l.warehouse.id = :warehouseId
                          AND l.locationType = :normal
                          AND l.status = :active
                        ORDER BY l.zone, l.shelf, l.warehouseCode
                        """, WarehouseLocations.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("normal", WarehouseLocationType.NORMAL)
                .setParameter("active", WarehouseStatus.ACTIVE)
                .setMaxResults(1)
                .getResultList())
                .map(l -> of(l, "Free storage location"));
    }

    private static Optional<WarehouseLocations> first(List<WarehouseLocations> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private static Suggestion of(WarehouseLocations l, String reason) {
        return new Suggestion(l.getId(), l.getWarehouseCode(), l.getZone(), l.getShelf(), reason);
    }
}
