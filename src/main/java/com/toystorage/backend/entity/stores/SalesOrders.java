package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.stores.PaymentMethod;
import com.toystorage.backend.enums.stores.SalesOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrders {

    /*
     * Khóa chính của đơn bán hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Store thực hiện bán hàng.
     *
     * Bảng warehouses đang lưu cả kho tổng và cửa hàng.
     * Với trường này, warehouse.type phải là STORE.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false
    )
    private Warehouses store;

    /*
     * Tên khách hàng.
     *
     * Có thể để null nếu là khách mua lẻ
     * và cửa hàng không cần lưu thông tin khách.
     */
    @Column(
            name = "customer_name",
            length = 200
    )
    private String customerName;

    /*
     * Tổng tiền của toàn bộ đơn bán hàng.
     *
     * Giá trị này bằng tổng subtotal
     * của tất cả SalesOrderItems.
     */
    @Builder.Default
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /*
     * Phương thức thanh toán.
     *
     * CASH:
     * Tiền mặt.
     *
     * BANK_TRANSFER:
     * Chuyển khoản ngân hàng.
     *
     * CARD:
     * Thanh toán bằng thẻ.
     *
     * E_WALLET:
     * Ví điện tử.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private PaymentMethod paymentMethod;

    /*
     * Trạng thái đơn bán hàng.
     *
     * DRAFT:
     * Đơn đang được tạo.
     *
     * PENDING_PAYMENT:
     * Đang chờ khách thanh toán.
     *
     * COMPLETED:
     * Đơn đã hoàn tất và Store bị trừ tồn.
     *
     * CANCELLED:
     * Đơn bị hủy.
     *
     * REFUNDED:
     * Đơn đã được hoàn tiền.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    /*
     * Nhân viên Store tạo đơn bán hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private Users createdBy;

    /*
     * Thời điểm tạo đơn bán hàng.
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
     * Mã nội bộ của đơn bán hàng.
     *
     * Ví dụ:
     * SO2026080001
     */
    @Column(
            name = "sales_orders_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String salesOrdersCode;

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
            status = SalesOrderStatus.DRAFT;
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}