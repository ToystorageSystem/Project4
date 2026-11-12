package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.entity.warehouses.Warehouses;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Hang hong / loi dang nam o khu hang hong (QUARANTINE / DAMAGED), ghi theo lo:
 * ma lo = ma chung tu nguon (phieu nhap, phieu tra hang, phieu kiem ke).
 * Hang nay KHONG duoc cong vao ton kha dung.
 */
@Entity
@Table(name = "damaged_stock_lots", indexes = {
        @Index(name = "idx_dsl_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_dsl_lot", columnList = "lot_code")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DamagedStockLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vi du: LOT-GR-000031, LOT-SR-000012, LOT-SC-20261201-W9-BOM. */
    @Column(name = "lot_code", nullable = false, length = 80)
    private String lotCode;

    /** GOODS_RECEIPT, STORE_RETURN, STOCK_COUNT. */
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_code", length = 80)
    private String sourceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocations location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private Users recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
