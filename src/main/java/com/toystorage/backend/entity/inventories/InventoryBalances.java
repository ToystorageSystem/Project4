package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.warehouses.Warehouses;
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

    /**
     * Kho hoặc cửa hàng đang chứa tồn kho.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_balances_warehouse"
            )
    )
    private Warehouses warehouse;

    /**
     * Vị trí cụ thể trong kho hoặc cửa hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_balances_location"
            )
    )
    private WarehouseLocations location;

    /**
     * Sản phẩm đang được lưu tại vị trí.
     */
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
     * Tồn thực tế / On-hand quantity.
     */
    @Column(
            name = "quantity",
            nullable = false
    )
    private Integer quantity;

    /**
     * Số lượng đã được giữ cho các nghiệp vụ khác,
     * ví dụ Stock Transfer.
     */
    @Column(
            name = "reserved_quantity",
            nullable = false
    )
    private Integer reservedQuantity;

    /**
     * Mức tồn tối thiểu tại địa điểm.
     *
     * Dùng để xác định trạng thái tồn kho:
     *
     * available = 0
     * -> OUT_OF_STOCK
     *
     * available > 0
     * và available <= minimumStockLevel
     * -> LOW_STOCK
     *
     * available > minimumStockLevel
     * -> IN_STOCK
     */
    @Column(
            name = "minimum_stock_level",
            nullable = false
    )
    private Integer minimumStockLevel;

    /**
     * Tồn khả dụng.
     *
     * availableQuantity = quantity - reservedQuantity
     */
    @Column(
            name = "available_quantity",
            nullable = false
    )
    private Integer availableQuantity;

    /**
     * Thời gian cập nhật tồn kho gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * Khởi tạo giá trị trước khi INSERT.
     */
    @PrePersist
    protected void onCreate() {

        if (quantity == null) {
            quantity = 0;
        }

        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }

        if (minimumStockLevel == null) {
            minimumStockLevel = 0;
        }

        calculateAvailableQuantity();

        updatedAt = LocalDateTime.now();
    }

    /**
     * Cập nhật tồn khả dụng và thời gian
     * trước khi UPDATE.
     */
    @PreUpdate
    protected void onUpdate() {

        calculateAvailableQuantity();

        updatedAt = LocalDateTime.now();
    }

    /**
     * Công thức:
     *
     * available = on-hand - reserved
     */
    private void calculateAvailableQuantity() {

        int currentQuantity =
                quantity != null
                        ? quantity
                        : 0;

        int currentReservedQuantity =
                reservedQuantity != null
                        ? reservedQuantity
                        : 0;

        availableQuantity =
                currentQuantity - currentReservedQuantity;
    }
}