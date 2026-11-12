package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Danh gia hieu suat cua Warehouse Manager doi voi nhan vien trong mot ky. */
@Entity
@Table(name = "staff_performance_reviews", indexes = {
        @Index(name = "idx_spr_staff", columnList = "staff_id"),
        @Index(name = "idx_spr_warehouse", columnList = "warehouse_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffPerformanceReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_code", nullable = false, unique = true, length = 50)
    private String reviewCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouses warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Users staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Users reviewer;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    /** Diem danh gia cua quan ly 1-5. */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /** Diem he thong (0-100) tai thoi diem danh gia, luu lai de doi chieu. */
    @Column(name = "system_score", precision = 5, scale = 1)
    private BigDecimal systemScore;

    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "strengths", length = 1000)
    private String strengths;

    @Column(name = "improvements", length = 1000)
    private String improvements;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
