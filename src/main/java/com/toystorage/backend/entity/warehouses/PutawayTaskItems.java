package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.warehouses.PutawayTaskItemStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "putaway_task_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PutawayTaskItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nhiệm vụ putaway chứa dòng sản phẩm này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "putaway_task_id", nullable = false)
    private PutawayTasks putawayTask;

    /**
     * Sản phẩm cần đưa lên kệ.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /**
     * Vị trí hàng đang nằm trước khi putaway.
     *
     * Thường là khu RECEIVING.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_location_id", nullable = false)
    private WarehouseLocations fromLocation;

    /**
     * Vị trí kệ đích.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_location_id", nullable = false)
    private WarehouseLocations toLocation;

    /**
     * Số lượng dự kiến cần đưa lên kệ.
     */
    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    /**
     * Số lượng thực tế đã đưa lên kệ.
     */
    @Builder.Default
    @Column(name = "putaway_quantity", nullable = false)
    private Integer putawayQuantity = 0;

    /**
     * Trạng thái của từng dòng sản phẩm.
     *
     * PENDING
     * IN_PROGRESS
     * COMPLETED
     * PARTIALLY_COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private PutawayTaskItemStatus status = PutawayTaskItemStatus.PENDING;

    /**
     * Mã nội bộ của chi tiết nhiệm vụ.
     */
    @Column(
            name = "putaway_task_items_code",
            nullable = false,
            length = 50
    )
    private String putawayTaskItemsCode;

    @PrePersist
    protected void onCreate() {
        if (putawayQuantity == null) {
            putawayQuantity = 0;
        }

        if (status == null) {
            status = PutawayTaskItemStatus.PENDING;
        }
    }
}