package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.DamagedGoodsStatus;
import com.toystorage.backend.enums.warehouses.DamagedGoodsSourceType;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "damaged_goods_reports",

        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_damaged_goods_reports_report_code",
                        columnNames = "report_code"
                ),
                @UniqueConstraint(
                        name = "uk_damaged_goods_reports_code",
                        columnNames = "damaged_goods_reports_code"
                )
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamagedGoodsReports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã báo cáo hàng lỗi.
     * Ví dụ: DGR000001.
     */
    @Column(name = "report_code", nullable = false, length = 50)
    private String reportCode;

    /**
     * Kho hoặc cửa hàng phát hiện hàng lỗi.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    /**
     * Nguồn phát hiện hàng lỗi.
     *
     * STORE_RECEIPT
     * GOODS_RECEIPT
     * STOCK_COUNT
     * SALES_RETURN
     * MANUAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private DamagedGoodsSourceType sourceType;

    /**
     * ID chứng từ nguồn.
     *
     * Ví dụ:
     * - ID phiếu nhận hàng tại Store.
     * - ID phiếu nhập kho.
     * - ID phiếu kiểm kê.
     */
    @Column(name = "source_id")
    private Long sourceId;

    /**
     * Trạng thái báo cáo.
     *
     * OPEN
     * PENDING_REVIEW
     * APPROVED
     * TRANSFER_CREATED
     * RESOLVED
     * REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private DamagedGoodsStatus status = DamagedGoodsStatus.REPORTED;

    /**
     * Người tạo báo cáo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private Users reportedBy;

    /**
     * Người xem xét báo cáo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Users reviewedBy;

    /**
     * Mô tả chung về tình trạng hàng lỗi.
     */
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Thời điểm tạo báo cáo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm báo cáo được xem xét.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Thời điểm hoàn tất xử lý.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Thời điểm cập nhật gần nhất.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Mã nội bộ của bản ghi.
     */
    @Column(
            name = "damaged_goods_reports_code",
            nullable = false,
            length = 50
    )
    private String damagedGoodsReportsCode;

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
            status = DamagedGoodsStatus.REPORTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}