package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.enums.warehouses.WarehouseLocationType;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "warehouse_locations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouse_locations_warehouse_code",
                        columnNames = {
                                "warehouse_id",
                                "warehouse_code"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_warehouse_locations_warehouse_id",
                        columnList = "warehouse_id"
                ),
                @Index(
                        name = "idx_warehouse_locations_type",
                        columnList = "location_type"
                ),
                @Index(
                        name = "idx_warehouse_locations_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_warehouse_locations_zone_shelf",
                        columnList = "zone, shelf"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseLocations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kho hoặc cửa hàng chứa vị trí này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_warehouse_locations_warehouse"
            )
    )
    private Warehouses warehouse;

    /**
     * Mã vị trí trong phạm vi một kho.
     *
     * Ví dụ:
     * A-01-01
     * A-01-02
     */
    @Column(
            name = "warehouse_code",
            nullable = false,
            length = 50
    )
    private String warehouseCode;

    /**
     * Mã định danh riêng của bản ghi vị trí.
     */
    @Column(
            name = "warehouse_locations_code",
            nullable = false,
            length = 50
    )
    private String warehouseLocationsCode;

    /**
     * Tên hiển thị của vị trí.
     *
     * Ví dụ:
     * Kệ A01
     * Khu nhận hàng số 1
     */
    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    /**
     * Khu vực lớn trong kho.
     *
     * Ví dụ:
     * ZONE-A
     */
    @Column(
            name = "zone",
            length = 50
    )
    private String zone;

    /**
     * Mã kệ.
     *
     * Ví dụ:
     * SHELF-01
     */
    @Column(
            name = "shelf",
            length = 50
    )
    private String shelf;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "location_type",
            nullable = false,
            length = 30
    )
    private WarehouseLocationType locationType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private WarehouseStatus status;

    @PrePersist
    protected void onCreate() {
        if (locationType == null) {
            locationType = WarehouseLocationType.NORMAL;
        }

        if (status == null) {
            status = WarehouseStatus.ACTIVE;
        }
    }
}