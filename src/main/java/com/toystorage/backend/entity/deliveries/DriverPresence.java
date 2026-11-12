package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Live state of a Delivery Staff: accepting new pickups, sharing GPS and the last known position. */
@Entity
@Table(name = "fleet_driver_presence",
        uniqueConstraints = @UniqueConstraint(name = "uk_fleet_presence_driver", columnNames = "driver_id"))
@Getter
@Setter
@NoArgsConstructor
public class DriverPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Users driver;

    /** false = the driver switched off "accepting new pickups"; auto dispatch skips him. */
    @Column(name = "available", nullable = false)
    private boolean available = true;

    @Column(name = "sharing", nullable = false)
    private boolean sharing = false;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 10, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}
