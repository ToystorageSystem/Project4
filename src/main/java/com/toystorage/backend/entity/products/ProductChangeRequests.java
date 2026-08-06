package com.toystorage.backend.entity.products;

import com.toystorage.backend.enums.products.ProductChangeRequestStatus;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_change_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_change_requests_code",
                        columnNames = "product_change_requests_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_change_requests_product_id",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_product_change_requests_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_product_change_requests_created_by",
                        columnList = "created_by"
                ),
                @Index(
                        name = "idx_product_change_requests_approved_by",
                        columnList = "approved_by"
                ),
                @Index(
                        name = "idx_product_change_requests_created_at",
                        columnList = "created_at"
                )
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductChangeRequests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Request code.
     *
     * Example:
     * PCR-000001
     */
    @Column(
            name = "product_change_requests_code",
            nullable = false,
            length = 50
    )
    private String productChangeRequestsCode;

    /**
     * Product requested to be changed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_change_requests_product"
            )
    )
    private Products product;

    /**
     * User who submitted the request.
     * Normally Business Staff.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_change_requests_created_by"
            )
    )
    private Users createdBy;

    /**
     * Business Manager who reviewed the request.
     *
     * Nullable until the request is reviewed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "approved_by",
            foreignKey = @ForeignKey(
                    name = "fk_product_change_requests_approved_by"
            )
    )
    private Users approvedBy;

    /**
     * Original product information before editing.
     *
     * Stored as JSON.
     */
    @Lob
    @Column(
            name = "old_value",
            columnDefinition = "LONGTEXT"
    )
    private String oldValue;

    /**
     * Requested new product information.
     *
     * Stored as JSON.
     */
    @Lob
    @Column(
            name = "new_value",
            columnDefinition = "LONGTEXT",
            nullable = false
    )
    private String newValue;

    /**
     * Reason why Business Staff wants to modify the product.
     */
    @Column(
            name = "request_reason",
            length = 500
    )
    private String requestReason;

    /**
     * Reason for rejection.
     */
    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    /**
     * Approval status.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ProductChangeRequestStatus status;

    /**
     * Time when the request was submitted.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Time when the request was reviewed.
     */
    @Column(
            name = "approved_at"
    )
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {

        if (status == null) {
            status = ProductChangeRequestStatus.PENDING;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

    }

}