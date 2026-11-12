package com.toystorage.backend.entity.stores;

import java.time.LocalDateTime;

import com.toystorage.backend.entity.products.Products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.toystorage.backend.exceptions.BadRequest;

/**
 * Stock of ONE product in ONE store (UNIQUE store_id + product_id).
 * Table created by hand (store_tables_create.sql) – column names must match exactly.
 */
@Entity
@Table(name = "store_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreStocks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_stocks_code", nullable = false, length = 80)
    private String code;

    /** = stores.warehouse_id */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private StoreShelves shelf;             // null = not on a shelf yet

    @Column(name = "shelf_quantity", nullable = false)
    private int shelfQuantity;

    @Column(name = "backroom_quantity", nullable = false)
    private int backroomQuantity;

    @Column(name = "min_shelf_quantity", nullable = false)
    private int minShelfQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** shelf + backroom (computed, not a column) */
    public int getTotalQuantity() {
        return shelfQuantity + backroomQuantity;
    }

        /** new stock row for a product this store has never had (shelf not assigned yet) */
    public static StoreStocks newRow(Long storeId, Products product) {
        StoreStocks s = new StoreStocks();
        LocalDateTime now = LocalDateTime.now();
        s.code = "SS-" + storeId + "-" + product.getProductsCode();
        s.storeId = storeId;
        s.product = product;
        s.shelf = null;
        s.shelfQuantity = 0;
        s.backroomQuantity = 0;
        s.minShelfQuantity = 0;
        s.createdAt = now;
        s.updatedAt = now;
        return s;
    }

    /** goods received from the warehouse go to the backroom first */
    public void addToBackroom(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        this.backroomQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

        /** shelf != null and shelf quantity at/below the minimum */
    public boolean isLowStock() {
        return shelf != null && shelfQuantity <= minShelfQuantity;
    }

    /**
     * Assign / change / remove the shelf (1 product = 1 shelf).
     * Changing shelf keeps shelfQuantity (goods move with the product).
     * Removing (null) is only allowed when nothing is left on the shelf.
     */
    public void assignShelf(StoreShelves newShelf) {
        if (newShelf == null) {
            if (shelfQuantity > 0) {
                throw new BadRequest("Move shelf stock to backroom first");
            }
        } else if (!newShelf.isActive()) {
            throw new BadRequest("Shelf is not active");
        }
        this.shelf = newShelf;
        touch();
    }

    /** backroom -> shelf */
    public void moveToShelf(int quantity) {
        requirePositive(quantity);
        if (shelf == null) {
            throw new BadRequest("Product has no shelf");
        }
        if (backroomQuantity < quantity) {
            throw new BadRequest("Not enough stock in backroom");
        }
        this.backroomQuantity -= quantity;
        this.shelfQuantity += quantity;
        touch();
    }

    /** shelf -> backroom */
    public void moveToBackroom(int quantity) {
        requirePositive(quantity);
        if (shelfQuantity < quantity) {
            throw new BadRequest("Not enough stock on shelf");
        }
        this.shelfQuantity -= quantity;
        this.backroomQuantity += quantity;
        touch();
    }

    public void changeMinShelfQuantity(int minShelfQuantity) {
        if (minShelfQuantity < 0) {
            throw new BadRequest("minShelfQuantity must be >= 0");
        }
        this.minShelfQuantity = minShelfQuantity;
        touch();
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BadRequest("quantity must be > 0");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

        /**
     * Sale: take from the shelf first, the rest from the backroom.
     * @return how many items came from the shelf
     */
    public int sell(int quantity) {
        requirePositive(quantity);
        if (getTotalQuantity() < quantity) {
            throw new BadRequest("Not enough stock");
        }
        int fromShelf = Math.min(shelfQuantity, quantity);
        this.shelfQuantity -= fromShelf;
        this.backroomQuantity -= quantity - fromShelf;
        touch();
        return fromShelf;
    }
}