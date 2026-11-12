package com.toystorage.backend.entity.suppliers;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Nhà cung cấp được đánh giá. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Suppliers supplier;

    /* Tổng số lần giao hàng đã ghi nhận. */
    @Builder.Default
    @Column(name = "total_deliveries", nullable = false)
    private Integer totalDeliveries = 0;

    /* Tổng số lần giao đúng hạn. */
    @Builder.Default
    @Column(name = "on_time_deliveries", nullable = false)
    private Integer onTimeDeliveries = 0;

    /* Tổng số lần giao thiếu hàng. */
    @Builder.Default
    @Column(name = "shortage_count", nullable = false)
    private Integer shortageCount = 0;

    /* Tổng số lần có hàng hư hỏng. */
    @Builder.Default
    @Column(name = "damaged_count", nullable = false)
    private Integer damagedCount = 0;

    /* Tỷ lệ chính xác, ví dụ 98.50 (%). */
    @Builder.Default
    @Column(name = "accuracy_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyRate = BigDecimal.ZERO;

    /* Thời điểm hệ thống thực hiện đánh giá. */
    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    /* Mã nội bộ theo đúng database. */
    @Column(name = "supplier_performance_code", nullable = false, length = 50)
    private String supplierPerformanceCode;

    @PrePersist
    protected void onCreate() {
        if (totalDeliveries == null) totalDeliveries = 0;
        if (onTimeDeliveries == null) onTimeDeliveries = 0;
        if (shortageCount == null) shortageCount = 0;
        if (damagedCount == null) damagedCount = 0;
        if (accuracyRate == null) accuracyRate = BigDecimal.ZERO;
        if (evaluatedAt == null) evaluatedAt = LocalDateTime.now();
    }
}
