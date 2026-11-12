package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "sales_order_items",

        /*
         * Một sản phẩm chỉ nên xuất hiện một lần
         * trong cùng một đơn bán hàng.
         *
         * Vì cần kiểm tra hai cột kết hợp:
         * sales_order_id + product_id
         * nên sử dụng uniqueConstraints.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sales_order_product",
                        columnNames = {
                                "sales_order_id",
                                "product_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderItems {

    /*
     * Khóa chính của dòng sản phẩm.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Đơn bán hàng chứa dòng sản phẩm này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sales_order_id",
            nullable = false
    )
    private SalesOrders salesOrder;

    /*
     * Sản phẩm được bán.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Products product;

    /*
     * Số lượng sản phẩm bán cho khách.
     */
    @Column(
            name = "quantity",
            nullable = false
    )
    private Integer quantity;

    /*
     * Giá bán của sản phẩm tại thời điểm bán.
     *
     * Không nên lấy lại trực tiếp từ bảng products
     * khi xem đơn cũ vì giá bán có thể đã thay đổi.
     */
    @Column(
            name = "unit_price",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal unitPrice;

    /*
     * Thành tiền của dòng sản phẩm.
     *
     * subtotal = quantity × unitPrice
     */
    @Column(
            name = "subtotal",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal subtotal;

    /*
     * Mã nội bộ của dòng sản phẩm.
     */
    @Column(
            name = "sales_order_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String salesOrderItemsCode;

    @PrePersist
    @PreUpdate
    protected void calculateSubtotal() {
        if (quantity == null || unitPrice == null) {
            subtotal = BigDecimal.ZERO;
            return;
        }

        subtotal = unitPrice.multiply(
                BigDecimal.valueOf(quantity)
        );
    }
}