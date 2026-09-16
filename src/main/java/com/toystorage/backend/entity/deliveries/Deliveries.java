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

    /** Mã chuyến giao hàng, ví dụ: DLV000001. */
    @Column(name = "shipment_code", nullable = false, unique = true, length = 50)
    private String shipmentCode;

    /**
     * Bảng kê đi hàng mà chuyến giao này thuộc về.
     *
     * DB:
     * deliveries.manifest_id -> shipment_manifests.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id")
    private ShipmentManifests manifest;

    /** Tài xế được phân công giao hàng. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Users driver;

    /** Kho/cửa hàng xuất phát. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_warehouse_id", nullable = false)
    private Warehouses fromWarehouse;

    /** Kho/cửa hàng nhận hàng. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    private Warehouses toWarehouse;

    /** Người bàn giao hàng cho tài xế. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handed_over_by")
    private Users handedOverBy;

    /** Thời điểm bàn giao hàng. */
    @Column(name = "handed_over_at")
    private LocalDateTime handedOverAt;

    /**
     * CREATED, ASSIGNED, READY_TO_SHIP,
     * IN_TRANSIT, ARRIVED, DELIVERED,
     * FAILED, CANCELLED.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus deliveryStatus = DeliveryStatus.CREATED;

    /** Thời điểm dự kiến bắt đầu lấy/giao hàng. */
    @Column(name = "expected_pickup_at")
    private LocalDateTime expectedPickupAt;

    /** Thời điểm dự kiến giao tới điểm nhận. */
    @Column(name = "expected_delivery_at")
    private LocalDateTime expectedDeliveryAt;
    /** Thời điểm tài xế bắt đầu chuyến giao. */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** Thời điểm giao hàng hoàn tất. */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}