package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouses_code",
                        columnNames = "warehouses_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_warehouses_type",
                        columnList = "type"
                ),
                @Index(
                        name = "idx_warehouses_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_warehouses_name",
                        columnList = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouses {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // WAREHOUSE CODE
    // =========================================================

    /**
     * Mã địa điểm.
     *
     * Ví dụ:
     * WH-001
     * STORE-001
     *
     * Mã phải duy nhất trong hệ thống.
     */
    @Column(
            name = "warehouses_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String warehousesCode;


    // =========================================================
    // NAME
    // =========================================================

    /**
     * Tên Kho tổng hoặc Cửa hàng.
     *
     * Ví dụ:
     * Kho Tổng TP.HCM
     * ToyStore Quận 1
     */
    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;


    // =========================================================
    // TYPE
    // =========================================================

    /**
     * Loại địa điểm.
     *
     * MAIN_WAREHOUSE = Kho tổng
     * STORE          = Cửa hàng
     *
     * Lưu ý:
     * Issue #25 ghi CENTRAL_WAREHOUSE,
     * nhưng hệ thống hiện tại đã sử dụng MAIN_WAREHOUSE.
     * Giữ MAIN_WAREHOUSE để không ảnh hưởng các nghiệp vụ cũ.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 30
    )
    private WarehouseType type;


    // =========================================================
    // ADDRESS
    // =========================================================

    /**
     * Địa chỉ Kho tổng hoặc Cửa hàng.
     */
    @Column(
            name = "address",
            length = 255
    )
    private String address;


    // =========================================================
    // PHONE
    // =========================================================

    /**
     * Số điện thoại liên hệ của địa điểm.
     */
    @Column(
            name = "phone",
            length = 20
    )
    private String phone;


    // =========================================================
    // STATUS
    // =========================================================

    /**
     * ACTIVE   = đang hoạt động
     * INACTIVE = đã ngừng hoạt động
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private WarehouseStatus status;


    // =========================================================
    // CREATED AT
    // =========================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    // =========================================================
    // UPDATED AT
    // =========================================================

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // PRE PERSIST
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = WarehouseStatus.ACTIVE;
        }
    }


    // =========================================================
    // PRE UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}