package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.shipments.ShipmentManifests;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;

import com.toystorage.backend.enums.deliveries.DeliveryStatus;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deliveries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // CODE
    // =====================================================

    @Column(
            name = "shipment_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String shipmentCode;


    // =====================================================
    // DRIVER
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Users driver;


    // =====================================================
    // ROUTE
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "from_warehouse_id",
            nullable = false
    )
    private Warehouses fromWarehouse;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "to_warehouse_id",
            nullable = false
    )
    private Warehouses toWarehouse;


    // =====================================================
    // MANIFEST
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id")
    private ShipmentManifests manifest;


    // =====================================================
    // HANDOVER
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handed_over_by")
    private Users handedOverBy;


    @Column(name = "handed_over_at")
    private LocalDateTime handedOverAt;


    // =====================================================
    // STATUS
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private DeliveryStatus deliveryStatus =
            DeliveryStatus.CREATED;


    // =====================================================
    // EXPECTED TIME
    // =====================================================

    @Column(name = "expected_pickup_at")
    private LocalDateTime expectedPickupAt;


    @Column(name = "expected_delivery_at")
    private LocalDateTime expectedDeliveryAt;


    // =====================================================
    // DRIVER ACCEPT / REJECT
    // =====================================================

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;


    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;


    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;


    // =====================================================
    // DELIVERY TIME
    // =====================================================

    @Column(name = "started_at")
    private LocalDateTime startedAt;


    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;


    // =====================================================
    // AUDIT
    // =====================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (deliveryStatus == null) {
            deliveryStatus =
                    DeliveryStatus.CREATED;
        }
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}