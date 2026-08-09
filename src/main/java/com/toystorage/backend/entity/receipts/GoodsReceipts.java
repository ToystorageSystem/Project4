package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.receipts.GoodsReceiptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipts {

    /*
     * Khóa chính của phiếu nhập hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã phiếu nhập kho.
     *
     * Ví dụ:
     * GR000001
     * GR2026080001
     *
     * Mỗi phiếu nhập phải có mã riêng.
     */
    @Column(
            name = "receipt_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String receiptCode;

    /*
     * Purchase Order được dùng để tạo phiếu nhập hàng.
     *
     * Luồng:
     * Purchase Order
     *      ↓
     * Supplier giao hàng
     *      ↓
     * Goods Receipt
     *
     * Một Purchase Order có thể phát sinh nhiều Goods Receipt
     * nếu nhà cung cấp giao hàng thành nhiều đợt.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_order_id",
            nullable = false
    )
    private PurchaseOrders purchaseOrder;

    /*
     * Kho tổng nhận hàng từ nhà cung cấp.
     *
     * Theo nghiệp vụ hiện tại, Supplier chỉ giao hàng về kho tổng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false
    )
    private Warehouses warehouse;

    /*
     * Trạng thái của phiếu nhập hàng.
     *
     * CREATED:
     * Phiếu đã được tạo nhưng chưa bắt đầu nhận hàng.
     *
     * RECEIVING:
     * Nhân viên kho đang kiểm nhận.
     *
     * INSPECTED:
     * Đã kiểm tra số lượng và tình trạng hàng.
     *
     * CONFIRMED:
     * Warehouse Manager đã xác nhận kết quả.
     *
     * COMPLETED:
     * Phiếu hoàn tất và tồn kho được tăng.
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
    private GoodsReceiptStatus status = GoodsReceiptStatus.CREATED;

    /*
     * Nhân viên kho trực tiếp nhận và kiểm hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private Users receivedBy;

    /*
     * Warehouse Manager hoặc người có quyền xác nhận nhập hàng.
     *
     * Khi chưa xác nhận thì trường này được phép null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Users confirmedBy;

    /*
     * Thời điểm hàng thực tế được tiếp nhận tại kho.
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /*
     * Thời điểm tạo phiếu nhập hàng.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /*
     * Thời điểm cập nhật phiếu gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Mã nội bộ của bản ghi Goods Receipt.
     *
     * Nếu receiptCode đã đủ dùng thì cột này có thể bị dư.
     * Tuy nhiên hiện tại mình vẫn giữ để khớp schema của bạn.
     */
    @Column(
            name = "goods_receipts_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String goodsReceiptsCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_confirmed_by")
    private Users inspectionConfirmedBy;

    @Column(name = "inspection_confirmed_at")
    private LocalDateTime inspectionConfirmedAt;
    /*
     * Tự động chạy trước khi INSERT.
     */
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
            status = GoodsReceiptStatus.CREATED;
        }
    }

    /*
     * Tự động cập nhật updatedAt trước khi UPDATE.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}