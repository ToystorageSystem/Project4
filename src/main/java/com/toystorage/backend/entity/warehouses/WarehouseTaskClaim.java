package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouse_task_claims",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouse_task_claim",
                        columnNames = {
                                "task_type",
                                "reference_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseTaskClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "task_type",
            nullable = false,
            length = 50
    )
    private WarehouseTaskType taskType;


    /*
     * ID của object nghiệp vụ:
     *
     * PUTAWAY          -> putaway_tasks.id
     * TRANSFER_PICKING -> stock_transfers.id
     * TRANSFER_PACKING -> stock_transfers.id
     * GOODS_RECEIVING  -> goods_receipts.id
     * STORE_RETURN     -> store_returns.id
     * STOCK_COUNT      -> stock_counts.id
     */
    @Column(
            name = "reference_id",
            nullable = false
    )
    private Long referenceId;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "claimed_by",
            nullable = false
    )
    private Users claimedBy;


    @Column(
            name = "claimed_at",
            nullable = false
    )
    private LocalDateTime claimedAt;


    @Column(name = "released_at")
    private LocalDateTime releasedAt;


    @PrePersist
    protected void onCreate() {

        if (claimedAt == null) {
            claimedAt = LocalDateTime.now();
        }
    }
}