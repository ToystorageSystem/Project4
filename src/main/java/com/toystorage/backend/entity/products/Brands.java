package com.toystorage.backend.entity.products;

import com.toystorage.backend.enums.products.CommonStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "brands",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_brands_code",
                        columnNames = "brands_code"
                ),
                @UniqueConstraint(
                        name = "uk_brands_name",
                        columnNames = "name"
                )
        },
        indexes = {
                @Index(
                        name = "idx_brands_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brands {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã thương hiệu.
     *
     * Ví dụ:
     * BRAND-001
     */
    @Column(
            name = "brands_code",
            nullable = false,
            length = 50
    )
    private String brandsCode;

    /**
     * Tên thương hiệu.
     * Ví dụ:
     * Royal Canin
     * Wanpy
     */
    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    /**
     * Quốc gia hoặc khu vực xuất xứ.
     */
    @Column(
            name = "country",
            length = 100
    )
    private String country;

    /**
     * Mô tả thương hiệu.
     */
    @Lob
    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private CommonStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = CommonStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}