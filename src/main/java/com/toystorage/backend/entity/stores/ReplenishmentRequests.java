package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.stores.ReplenishmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "replenishment_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplenishmentRequests {

    /*
     * Khóa chính của yêu cầu bổ sung hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã yêu cầu bổ sung hàng.
     *
     * Ví dụ:
     * RR2026080001
     */
    @Column(
            name = "request_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String requestCode;

    /*
     * Store cần được bổ sung hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false
    )
    private Warehouses store;

    /*
     * Sản phẩm cần bổ sung.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Products product;

    /*
     * Số lượng hiện tại của sản phẩm tại Store
     * ở thời điểm tạo yêu cầu.
     *
     * Trường này dùng để lưu dữ liệu lịch sử.
     */
    @Column(
            name = "current_quantity",
            nullable = false
    )
    private Integer currentQuantity;

    /*
     * Số lượng Store đề nghị được bổ sung.
     */
    @Column(
            name = "requested_quantity",
            nullable = false
    )
    private Integer requestedQuantity;

    /*
     * Lý do yêu cầu bổ sung.
     *
     * Ví dụ:
     * - Sản phẩm sắp hết.
     * - Sản phẩm bán nhanh.
     * - Chuẩn bị chương trình khuyến mãi.
     */
    @Column(
            name = "reason",
            length = 500
    )
    private String reason;

    /*
     * Trạng thái yêu cầu bổ sung.
     *
     * PENDING:
     * Chờ Business xem xét.
     *
     * APPROVED:
     * Yêu cầu được duyệt.
     *
     * REJECTED:
     * Yêu cầu bị từ chối.
     *
     * TRANSFER_CREATED:
     * Business đã tạo Stock Transfer.
     *
     * COMPLETED:
     * Store đã nhận đủ hàng.
     *
     * CANCELLED:
     * Yêu cầu bị hủy.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ReplenishmentStatus status = ReplenishmentStatus.PENDING;

    /*
     * Người tạo yêu cầu.
     *
     * Thường là Store Staff hoặc Store Manager.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private Users createdBy;

    /*
     * Business Staff hoặc Business Manager
     * xem xét yêu cầu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Users reviewedBy;

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
     * Thời điểm Business xem xét yêu cầu.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /*
     * Thời điểm cập nhật gần nhất.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /*
     * Mã nội bộ của bản ghi.
     *
     * Nếu requestCode đã đủ dùng thì cột này
     * có thể bị trùng ý nghĩa.
     */
    @Column(
            name = "replenishment_requests_code",
            nullable = false,
            unique = true,
            length = 255
    )
    private String replenishmentRequestsCode;

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
            status = ReplenishmentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}