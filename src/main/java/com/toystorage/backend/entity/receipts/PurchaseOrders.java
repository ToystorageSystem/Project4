package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.suppliers.SupplierInvoices;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.suppliers.SupplierResponseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrders {

    /*
     * Khóa chính của đơn mua hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã đơn mua hàng do hệ thống sinh.
     *
     * Ví dụ:
     * PO000001
     * PO2026080001
     */
    @Column(
            name = "order_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String orderCode;

    /*
     * Nhà cung cấp nhận đơn đặt hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false
    )
    private Suppliers supplier;

    /*
     * Kho tổng dự kiến nhận hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "warehouse_id",
            nullable = false
    )
    private Warehouses warehouse;

    /*
     * Trạng thái đơn mua hàng.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    /*
     * Business Staff tạo đơn mua hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private Users createdBy;

    /*
     * Business Manager duyệt đơn.
     *
     * Null nếu đơn chưa được duyệt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Users approvedBy;

    /*
     * Thời điểm Business Manager duyệt đơn.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /*
     * Ngày nhà cung cấp dự kiến giao hàng.
     */
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /*
     * Ghi chú chung của đơn mua hàng.
     */
    @Lob
    @Column(
            name = "note",
            columnDefinition = "TEXT"
    )
    private String note;

    /*
     * Lý do từ chối đơn.
     */
    @Lob
    @Column(
            name = "rejection_reason",
            columnDefinition = "TEXT"
    )
    private String rejectionReason;

    /*
     * Lý do hủy đơn.
     *
     * Ví dụ:
     * - Nhà cung cấp hết hàng.
     * - Sản phẩm ngừng sản xuất.
     * - Giá thay đổi.
     */
    @Lob
    @Column(
            name = "cancel_reason",
            columnDefinition = "TEXT"
    )
    private String cancelReason;

    /*
     * Danh sách hóa đơn liên quan đến đơn mua hàng.
     *
     * Một đơn mua hàng có thể có nhiều hóa đơn
     * nếu nhà cung cấp giao hàng thành nhiều đợt.
     */
    @OneToMany(
            mappedBy = "purchaseOrder",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<SupplierInvoices> supplierInvoices =
            new ArrayList<>();
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "supplier_response_status",
            length = 30
    )
    private SupplierResponseStatus supplierResponseStatus =
            SupplierResponseStatus.PENDING_RESPONSE;

    /*
     * Ghi chú phản hồi.
     *
     * Ví dụ:
     * - Chỉ còn 50 sản phẩm.
     * - Giá tăng lên 120.000đ.
     * - Giao sau 5 ngày.
     */
    @Lob
    @Column(
            name = "supplier_response_note",
            columnDefinition = "TEXT"
    )
    private String supplierResponseNote;

    /*
     * Thời điểm nhận phản hồi.
     */
    @Column(name = "supplier_responded_at")
    private LocalDateTime supplierRespondedAt;
    /*
     * Thời điểm tạo đơn.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /*
     * Thời điểm cập nhật đơn gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Thêm hóa đơn vào đơn mua hàng
     * và đồng thời thiết lập quan hệ hai chiều.
     */
    public void addSupplierInvoice(SupplierInvoices invoice) {
        if (invoice == null) {
            return;
        }

        supplierInvoices.add(invoice);
        invoice.setPurchaseOrder(this);
    }

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
            status = PurchaseOrderStatus.DRAFT;
        }
        if (supplierResponseStatus == null) {
            supplierResponseStatus =
                    SupplierResponseStatus.PENDING_RESPONSE;
        }

    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}