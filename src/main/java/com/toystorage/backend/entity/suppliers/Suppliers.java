package com.toystorage.backend.entity.suppliers;

import com.toystorage.backend.enums.products.CommonStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_suppliers_code",
                        columnNames = "suppliers_code"
                ),
                @UniqueConstraint(
                        name = "uk_suppliers_tax_code",
                        columnNames = "tax_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_suppliers_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_suppliers_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_suppliers_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_suppliers_phone",
                        columnList = "phone"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suppliers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mã nhà cung cấp do hệ thống sinh.
     *
     * Ví dụ: SUP-000001
     */
    @Column(
            name = "suppliers_code",
            nullable = false,
            length = 50
    )
    private String suppliersCode;

    /*
     * Tên nhà cung cấp.
     */
    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    /*
     * Mã số thuế của nhà cung cấp.
     */
    @Column(
            name = "tax_code",
            length = 50
    )
    private String taxCode;

    /*
     * Người liên hệ chính.
     */
    @Column(
            name = "contact_person",
            length = 150
    )
    private String contactPerson;

    /*
     * Chức vụ của người liên hệ.
     */
    @Column(
            name = "contact_position",
            length = 100
    )
    private String contactPosition;

    @Column(
            name = "phone",
            length = 30
    )
    private String phone;

    @Column(
            name = "email",
            length = 150
    )
    private String email;

    @Column(
            name = "address",
            length = 255
    )
    private String address;

    /*
     * Ghi chú về nhà cung cấp.
     */
    @Lob
    @Column(
            name = "note",
            columnDefinition = "TEXT"
    )
    private String note;

    /*
     * ACTIVE: Nhà cung cấp đang hoạt động.
     * INACTIVE: Nhà cung cấp đã bị ẩn hoặc ngừng hợp tác.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private CommonStatus status = CommonStatus.ACTIVE;

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