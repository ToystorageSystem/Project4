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
 * Shelves are seeded – this phase has no create/update/delete API.
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
}