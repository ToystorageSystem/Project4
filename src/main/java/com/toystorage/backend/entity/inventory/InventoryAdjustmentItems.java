package com.toystorage.backend.entity.inventory;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_adjustment_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_adjustment_id", nullable = false)
    private InventoryAdjustment inventoryAdjustment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocations location;

    /** Tồn trước điều chỉnh. */
    @Column(name = "old_quantity", nullable = false)
    private Integer oldQuantity;

    /** Số lượng tăng/giảm; số âm là giảm, số dương là tăng. */
    @Column(name = "adjustment_quantity", nullable = false)
    private Integer adjustmentQuantity;

    /** Tồn sau điều chỉnh. */
    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    @Column(name = "inventory_adjustment_items_code", nullable = false, length = 50)
    private String inventoryAdjustmentItemsCode;
}
