package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouses",
        /**
         * những cái này làm vì nhóm mình không có tạo bảng trực tiếp mà dùng hibernate
        */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouses_code",
                        columnNames = "warehouses_code"
                )
        },
        /**
         * Index (chỉ mục) trong SQL là một cấu trúc dữ liệu giúp tăng tốc độ tìm kiếm và truy vấn dữ liệu.
        */
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã kho hoặc cửa hàng.
     *
     * Ví dụ:
     * WH-001
     * STORE-001
     */
    @Column(
            name = "warehouses_code",
            nullable = false,
            length = 50
    )
    private String warehousesCode;

    /**
     * Tên kho hoặc cửa hàng.
     */
    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    /**
     * Phân biệt kho tổng và cửa hàng.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false,
            length = 30
    )
    private WarehouseType type;

    @Column(
            name = "address",
            length = 255
    )
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private WarehouseStatus status;

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

        updatedAt = now;

        if (status == null) {
            status = WarehouseStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}