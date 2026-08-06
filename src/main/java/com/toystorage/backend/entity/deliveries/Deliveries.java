package com.toystorage.backend.entity.deliveries;

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

    /** CREATED, ASSIGNED, IN_TRANSIT, DELIVERED, FAILED, CANCELLED. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus deliveryStatus = DeliveryStatus.CREATED;

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
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
