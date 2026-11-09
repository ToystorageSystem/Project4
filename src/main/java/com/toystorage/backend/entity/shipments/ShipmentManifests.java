package com.toystorage.backend.entity.shipments;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.shipments.ShipmentManifestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipment_manifests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentManifests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã bảng kê đi hàng.
     * Ví dụ: SM2026080001
     */
    @Column(
            name = "manifest_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String manifestCode;

    /*
     * Điểm xuất hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_warehouse_id", nullable = false)
    private Warehouses fromWarehouse;

    /*
     * Điểm nhận hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    private Warehouses toWarehouse;

    /*
     * Trạng thái duyệt bảng kê.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentManifestStatus status =
            ShipmentManifestStatus.CREATED;

    /*
     * Người tạo bảng kê.
     * Có thể là hệ thống hoặc Warehouse Staff.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    /*
     * Business duyệt bảng kê.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Users approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Lob
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(
            mappedBy = "manifest",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ShipmentManifestTransfer> transfers =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "manifest",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ShipmentManifestPackage> packages =
            new ArrayList<>();

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
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = ShipmentManifestStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}