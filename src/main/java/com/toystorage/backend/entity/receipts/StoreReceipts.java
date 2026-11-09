package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.receipts.StoreReceiptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReceipts {

    /*
     * Khóa chính của phiếu nhận hàng tại Store.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Phiếu điều chuyển hàng từ kho đến Store.
     *
     * Một Store Receipt được tạo dựa trên một Stock Transfer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stock_transfer_id",
            nullable = false
    )
    private StockTransfer stockTransfer;

    /*
     * Store nhận hàng.
     *
     * Bảng warehouses đang lưu cả kho tổng và Store,
     * vì vậy storeId cũng tham chiếu đến bảng warehouses.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false
    )
    private Warehouses store;

    /*
     * Trạng thái phiếu nhận hàng.
     *
     * PENDING:
     * Hàng chưa đến hoặc Store chưa bắt đầu kiểm.
     *
     * RECEIVING:
     * Store đang kiểm hàng.
     *
     * INSPECTED:
     * Đã kiểm số lượng, seal và tình trạng kiện.
     *
     * CONFIRMED:
     * Store Manager đã xác nhận kết quả.
     *
     * COMPLETED:
     * Store đã nhận xong và tồn Store được tăng.
     *
     * REJECTED:
     * Store từ chối nhận hàng.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private StoreReceiptStatus status = StoreReceiptStatus.PENDING;

    /*
     * Nhân viên Store trực tiếp nhận và kiểm hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private Users receivedBy;

    /*
     * Store Manager hoặc người có quyền xác nhận nhận hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Users confirmedBy;

    /*
     * Thời điểm Store hoàn thành nhận hàng.
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /*
     * Thời điểm tạo phiếu nhận hàng.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /*
     * Thời điểm cập nhật gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Mã nội bộ của phiếu nhận hàng.
     */
    @Column(
            name = "store_receipts_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String storeReceiptsCode;

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
            status = StoreReceiptStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}