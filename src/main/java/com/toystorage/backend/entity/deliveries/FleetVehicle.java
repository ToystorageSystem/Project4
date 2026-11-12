package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A delivery vehicle. Each Delivery Staff drives at most one active vehicle. */
@Entity
@Table(name = "fleet_vehicles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fleet_vehicle_plate", columnNames = "plate"),
                @UniqueConstraint(name = "uk_fleet_vehicle_driver", columnNames = "driver_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate", nullable = false, length = 30)
    private String plate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Users driver;

    /** How many packages the vehicle can carry at the same time. */
    @Column(name = "capacity_packages", nullable = false)
    private Integer capacityPackages;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
