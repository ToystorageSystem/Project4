package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** San pham duoc gan co dinh cho mot vi tri / khu vuc trong kho (hang nao de o dau). */
@Entity
@Table(name = "warehouse_location_products",
        uniqueConstraints = @UniqueConstraint(name = "uk_wlp_location_product", columnNames = {"location_id", "product_id"}),
        indexes = @Index(name = "idx_wlp_product", columnList = "product_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarehouseLocationProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocations location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private Users assignedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
