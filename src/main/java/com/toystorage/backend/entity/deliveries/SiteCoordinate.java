package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** GPS coordinates of a warehouse or store, used to find the nearest driver and to plan routes. */
@Entity
@Table(name = "fleet_site_coordinates",
        uniqueConstraints = @UniqueConstraint(name = "uk_fleet_site_warehouse", columnNames = "warehouse_id"))
@Getter
@Setter
@NoArgsConstructor
public class SiteCoordinate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Users updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
