package com.toystorage.backend.entity.stores;

import java.time.LocalDateTime;

import com.toystorage.backend.enums.stores.ShelfStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A shelf in a store. Table created by hand (store_tables_create.sql).
 * Store Manager creates / renames / deactivates shelves (StoreShelfAdminService, 2026-11-13).
 * No delete: a shelf is kept for the stock history.
 */
@Entity
@Table(name = "store_shelves")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreShelves {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_shelves_code", nullable = false, length = 50)
    private String code;

    /** = stores.warehouse_id */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShelfStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return status == ShelfStatus.ACTIVE;
    }

    public static StoreShelves create(Long storeId, String code, String name, String description) {
        StoreShelves shelf = new StoreShelves();
        LocalDateTime now = LocalDateTime.now();
        shelf.storeId = storeId;
        shelf.code = code;
        shelf.name = name;
        shelf.description = description;
        shelf.status = ShelfStatus.ACTIVE;
        shelf.createdAt = now;
        shelf.updatedAt = now;
        return shelf;
    }

    public void rename(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(ShelfStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}