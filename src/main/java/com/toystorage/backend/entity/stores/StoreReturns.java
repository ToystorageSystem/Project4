package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.stores.StoreReturnStatus;
import com.toystorage.backend.enums.stores.StoreReturnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReturns {

    /*
     * Khóa chính của phiếu trả hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã phiếu trả hàng.
     *
     * Ví dụ:
     * SR000001
     */
    @Column(
            name = "return_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String returnCode;

    /*
     * Phiếu điều chuyển từ Store về kho.
     *
     * Sau khi yêu cầu trả hàng được xác nhận,
     * hệ thống tạo Stock Transfer có hướng:
     *
     * Store → Warehouse.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stock_transfer_id",
            nullable = false,
            unique = true
    )
    private StockTransfer stockTransfer;

    /*
     * Store trả hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false
    )
    private Warehouses store;

    /*
     * Kho tổng nhận hàng trả.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false
    )
    private Warehouses warehouse;

    /*
     * Loại trả hàng.
     *
     * DAMAGED:
     * Trả hàng lỗi.
     *
     * SURPLUS:
     * Trả hàng Store nhận dư.
     *
     * RECALL:
     * Thu hồi sản phẩm.
     *
     * SLOW_MOVING:
     * Hàng bán chậm.
     *
     * WRONG_TRANSFER:
     * Kho điều sai hàng.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "return_type",
            nullable = false,
            length = 30
    )
    private StoreReturnType returnType;

    /*
     * Lý do chi tiết của việc trả hàng.
     *
     * Đây là trường bắt buộc vì Store → Warehouse
     * là luồng ngoại lệ.
     */
    @Lob
    @Column(
            name = "reason",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String reason;

    /*
     * Trạng thái phiếu trả hàng.
     *
     * DRAFT:
     * Store đang tạo yêu cầu.
     *
     * PENDING_APPROVAL:
     * Chờ Business hoặc Warehouse duyệt.
     *
     * APPROVED:
     * Yêu cầu đã được xác nhận.
     * Tại bước này Store bị trừ tồn ngay.
     *
     * PACKING:
     * Store đang đóng hàng.
     *
     * ISSUED:
     * Store đã bàn giao hàng.
     *
     * SHIPPED:
     * Hàng đang được vận chuyển về kho.
     *
     * RECEIVED:
     * Kho đã nhận và kiểm hàng.
     *
     * CANCELLED:
     * Phiếu bị hủy.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private StoreReturnStatus status = StoreReturnStatus.DRAFT;

    /*
     * Người tạo yêu cầu trả hàng.
     *
     * Có thể là Store Manager hoặc Business.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private Users createdBy;

    /*
     * Business hoặc Warehouse xác nhận yêu cầu.
     *
     * Khi người này xác nhận:
     * Store Inventory bị trừ ngay để số hàng đó
     * không thể tiếp tục được bán.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Users approvedBy;

    /*
     * Nhân viên Store xác nhận xuất hàng thực tế.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private Users issuedBy;

    /*
     * Thời điểm tạo yêu cầu.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /*
     * Thời điểm Business hoặc Warehouse duyệt.
     *
     * Tại thời điểm này Store bị trừ tồn.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /*
     * Thời điểm Store bàn giao hàng cho Driver.
     */
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    /*
     * Thời điểm kho nhận hàng trả.
     *
     * Kho chỉ tăng tồn sau khi nhận và kiểm tra hoàn tất.
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /*
     * Thời điểm cập nhật gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Mã nội bộ của phiếu trả hàng.
     */
    @Column(
            name = "store_returns_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String storeReturnsCode;

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
            status = StoreReturnStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}