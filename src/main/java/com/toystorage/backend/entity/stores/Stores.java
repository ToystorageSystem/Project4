package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.warehouses.Warehouses;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stores {
    @Id
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "warehouse_id", foreignKey = @ForeignKey(name = "fk_stores_warehouse"))
    private Warehouses warehouse;

    @Column(name = "store_manager_name", length = 150)
    private String storeManagerName;

    @Column(name = "opening_hours", length = 100)
    private String openingHours;

    @Column(name = "store_phone", length = 20)
    private String storePhone;
}
