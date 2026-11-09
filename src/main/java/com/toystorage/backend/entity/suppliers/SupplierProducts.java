package com.toystorage.backend.entity.suppliers;

import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "supplier_products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_supplier_products_supplier_product",
                        columnNames = {
                                "supplier_id",
                                "product_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_supplier_products_code",
                        columnNames = "supplier_products_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_supplier_products_supplier_id",
                        columnList = "supplier_id"
                ),
                @Index(
                        name = "idx_supplier_products_product_id",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_supplier_products_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SupplierProducts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã liên kết nhà cung cấp và sản phẩm.
     */
    @Column(
            name = "supplier_products_code",
            nullable = false,
            length = 50
    )
    private String supplierProductsCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_supplier_products_supplier"
            )
    )
    private Suppliers supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_supplier_products_product"
            )
    )
    private Products product;
    /**
    * Mã sản phẩm do nhà cung cấp sử dụng.
    */
   @Column(
           name = "supplier_product_code",
           length = 100
   )
   private String supplierProductCode;

    /**
     * Giá sản phẩm do nhà cung cấp này báo.
     */
    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal purchasePrice;

    /**
     * Số ngày dự kiến từ khi đặt hàng đến khi giao.
     */
    @Column(
            name = "lead_time_days",
            nullable = false
    )
    private Integer leadTimeDays;

    /**
     * Số lượng đặt tối thiểu.
     */
    @Column(
            name = "minimum_order_quantity",
            nullable = false
    )
    private Integer minimumOrderQuantity;
    /**
    * Đánh dấu nhà cung cấp mặc định của sản phẩm.
    */
   @Builder.Default
   @Column(
           name = "is_default",
           nullable = false
   )
   private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private CommonStatus status = CommonStatus.ACTIVE;


    @PrePersist
    protected void onCreate() {
        if (minimumOrderQuantity == null) {
            minimumOrderQuantity = 1;
        }

        if (leadTimeDays == null) {
            leadTimeDays = 0;
        }

        if (status == null) {
            status = CommonStatus.ACTIVE;
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }
}