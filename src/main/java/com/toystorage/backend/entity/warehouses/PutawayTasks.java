package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.enums.warehouses.PutawayTaskStatus;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "putaway_tasks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_putaway_tasks_code",
                        columnNames = "putaway_tasks_code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PutawayTasks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Phiếu nhập hàng phát sinh nhiệm vụ đưa hàng lên kệ.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipts goodsReceipt;

    /**
     * Kho thực hiện putaway.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    /**
     * Trạng thái nhiệm vụ.
     *
     * PENDING
     * IN_PROGRESS
     * COMPLETED
     * CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private PutawayTaskStatus status = PutawayTaskStatus.PENDING;

    /**
     * Nhân viên kho được giao thực hiện.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private Users assignedTo;

    /**
     * Người tạo nhiệm vụ.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    /**
     * Thời điểm tạo nhiệm vụ.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm hoàn tất putaway.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Mã nhiệm vụ putaway.
     */
    @Column(
            name = "putaway_tasks_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String putawayTasksCode;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status =PutawayTaskStatus.PENDING;
        }
    }
}