package com.toystorage.backend.entity.products;

import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_products_barcode",
                        columnNames = "barcode"
                ),
                @UniqueConstraint(
                        name = "uk_products_code",
                        columnNames = "products_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_products_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_products_category_id",
                        columnList = "category_id"
                ),
                @Index(
                        name = "idx_products_brand_id",
                        columnList = "brand_id"
                ),
                @Index(
                        name = "idx_products_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_products_created_by",
                        columnList = "created_by"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã sản phẩm nội bộ.
     *
     * Ví dụ:
     * PROD-000001
     */
    @Column(
            name = "products_code",
            nullable = false,
            length = 50
    )
    private String productsCode;

    /**
     * Mã barcode được quét khi nhập, xuất và kiểm kho.
     */
    @Column(
            name = "barcode",
            nullable = false,
            length = 100
    )
    private String barcode;

    @Column(
            name = "image_url",
            length = 500
    )
    private String imageUrl;

    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_products_category"
            )
    )
    private Categories category;

    /**
     * Thương hiệu có thể để null nếu chưa xác định.
     *
     * Đây là cột bổ sung so với SQL hiện tại.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "brand_id",
            foreignKey = @ForeignKey(
                    name = "fk_products_brand"
            )
    )
    private Brands brand;

    /**
     * Đơn vị cơ sở.
     *
     * Ví dụ:
     * PIECE
     * BAG
     * BOX
     * BOTTLE
     */
    @Column(
            name = "base_unit",
            nullable = false,
            length = 30
    )
    private String baseUnit;

    /**
     * Giá nhập tham chiếu của sản phẩm.
     */
    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal purchasePrice;

    /**
     * Giá bán của sản phẩm.
     */
    @Column(
            name = "selling_price",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ProductStatus status;

    /**
     * Business Staff tạo sản phẩm.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_products_created_by"
            )
    )
    private Users createdBy;

    /**
     * Business Manager duyệt sản phẩm.
     * Khi sản phẩm đang PENDING_CREATE thì trường này có thể null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "approved_by",
            foreignKey = @ForeignKey(
                    name = "fk_products_approved_by"
            )
    )
    private Users approvedBy;

    /**
     * Lý do từ chối nếu sản phẩm không được duyệt.
     */
    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

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
            status = ProductStatus.PENDING_CREATE;
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