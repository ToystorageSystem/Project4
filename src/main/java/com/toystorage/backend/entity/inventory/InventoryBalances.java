package com.toystorage.backend.entity.inventory;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_balances_warehouse_location_product",
                        columnNames = {
                                "warehouse_id",
                                "location_id",
                                "product_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_inventory_balances_warehouse_id",
                        columnList = "warehouse_id"
                ),
                @Index(
                        name = "idx_inventory_balances_location_id",
                        columnList = "location_id"
                ),
                @Index(
                        name = "idx_inventory_balances_product_id",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_inventory_balances_warehouse_product",
                        columnList = "warehouse_id, product_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryBalances {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã bản ghi tồn kho.
     */
    @Column(
            name = "inventory_balances_code",
            nullable = false,
            length = 50
    )
    private String inventoryBalancesCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_balances_warehouse"
            )
    )
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_balances_location"
            )
    )
    private WarehouseLocations location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_balances_product"
            )
    )
    private Products product;

    /**
     * Tổng số lượng vật lý tại vị trí.
     */
    @Column(
            name = "quantity",
            nullable = false
    )
    private Integer quantity;

    /**
     * Số lượng đã được giữ chỗ cho phiếu khác.
     */
    @Column(
            name = "reserved_quantity",
            nullable = false
    )
    private Integer reservedQuantity;

    /**
     * Số lượng có thể tiếp tục sử dụng.
     *
     * availableQuantity = quantity - reservedQuantity
     */
    @Column(
            name = "available_quantity",
            nullable = false
    )
    private Integer availableQuantity;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (quantity == null) {
            quantity = 0;
        }

        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }

        calculateAvailableQuantity();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateAvailableQuantity();
        updatedAt = LocalDateTime.now();
    }

    private void calculateAvailableQuantity() {
        int currentQuantity = quantity == null ? 0 : quantity;
        int currentReservedQuantity =
                reservedQuantity == null ? 0 : reservedQuantity;

        availableQuantity =
                currentQuantity - currentReservedQuantity;
    }
}