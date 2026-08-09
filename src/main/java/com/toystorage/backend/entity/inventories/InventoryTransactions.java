package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.enums.inventories.InventoryTransactionType;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.inventories.InventoryReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_transactions",
        indexes = {
                @Index(
                        name = "idx_inventory_transactions_warehouse_id",
                        columnList = "warehouse_id"
                ),
                @Index(
                        name = "idx_inventory_transactions_location_id",
                        columnList = "location_id"
                ),
                @Index(
                        name = "idx_inventory_transactions_product_id",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_inventory_transactions_type",
                        columnList = "transaction_type"
                ),
                @Index(
                        name = "idx_inventory_transactions_reference",
                        columnList = "reference_type, reference_id"
                ),
                @Index(
                        name = "idx_inventory_transactions_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_inventory_transactions_performed_by",
                        columnList = "performed_by"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã giao dịch tồn kho.
     */
    @Column(
            name = "inventory_transactions_code",
            nullable = false,
            length = 50
    )
    private String inventoryTransactionsCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_transactions_warehouse"
            )
    )
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "location_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_transactions_location"
            )
    )
    private WarehouseLocations location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_transactions_product"
            )
    )
    private Products product;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 40
    )
    private InventoryTransactionType transactionType;

    /**
     * Lượng thay đổi.
     *
     * Ví dụ:
     * +10: tăng 10
     * -5: giảm 5
     */
    @Column(
            name = "quantity_change",
            nullable = false
    )
    private Integer quantityChange;

    @Column(
            name = "quantity_before",
            nullable = false
    )
    private Integer quantityBefore;

    @Column(
            name = "quantity_after",
            nullable = false
    )
    private Integer quantityAfter;

    /**
     * ID của chứng từ gây ra biến động.
     *
     * Ví dụ:
     * StockTransfer ID = 15
     */
    @Column(
            name = "reference_id",
            nullable = false
    )
    private Long referenceId;

    /**
     * Loại chứng từ tham chiếu.
     *
     * Ví dụ:
     * STOCK_TRANSFER
     * GOODS_RECEIPT
     * STORE_RETURN
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "reference_type",
            nullable = false,
            length = 50
    )
    private InventoryReferenceType referenceType;

    /**
     * Người thực hiện nghiệp vụ làm thay đổi tồn kho.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "performed_by",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_inventory_transactions_performed_by"
            )
    )
    private Users performedBy;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}