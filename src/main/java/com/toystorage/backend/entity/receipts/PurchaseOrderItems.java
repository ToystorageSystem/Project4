package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.suppliers.SupplierItemResponseStatus;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(
        name = "purchase_order_items",

        /*
         * Một sản phẩm chỉ nên xuất hiện một lần
         * trong cùng một Purchase Order.
         *
         * Ví dụ:
         * purchase_order_id = 1, product_id = 10
         * không được lưu lặp lại hai dòng.
         *
         * Đây là trường hợp cần kiểm tra hai cột kết hợp,
         * nên dùng uniqueConstraints.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_purchase_order_product",
                        columnNames = {
                                "purchase_order_id",
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
public class PurchaseOrderItems {

    /*
     * Khóa chính của dòng sản phẩm trong đơn mua.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Purchase Order chứa dòng sản phẩm này.
     *
     * Một Purchase Order có nhiều PurchaseOrderItems.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_order_id",
            nullable = false
    )
    private PurchaseOrders purchaseOrder;

    /*
     * Sản phẩm được đặt mua.
     *
     * Một sản phẩm có thể xuất hiện trong nhiều Purchase Order khác nhau.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Products product;

    /*
     * Số lượng Business đặt mua từ nhà cung cấp.
     *
     * Ví dụ:
     * orderedQuantity = 100
     */
    @NotNull
    @Min(1)
    @Column(
            name = "ordered_quantity",
            nullable = false
    )
    private Integer orderedQuantity;
    /*
     * Tổng số lượng đã thực nhận từ nhà cung cấp.
     *
     * Khi mới tạo đơn:
     * receivedQuantity = 0eceived_quantity
     *
     * Nếu nhà cung cấp giao thành nhiều lần,
     * giá trị này được cộng dồn sau mỗi lần nhận hàng.
     */
    @Min(0)
    @Builder.Default
    @Column(
            name = "received_quantity",
            nullable = false
    )
    private Integer receivedQuantity = 0;
    /*
     * Giá mua của sản phẩm tại thời điểm tạo đơn.
     *
     * Không nên chỉ lấy giá trực tiếp từ supplier_products
     * mỗi khi xem lại đơn, vì giá nhà cung cấp có thể thay đổi.
     *
     * Purchase Order cũ phải giữ nguyên giá lịch sử.
     */
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(
            name = "unit_price",
            nullable = false,
            precision = 18,
            scale = 2
    )
    private BigDecimal unitPrice;

    /*
     * Mã nội bộ của dòng sản phẩm trong đơn mua hàng.
     */
    @Column(
            name = "purchase_order_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String purchaseOrderItemsCode;
    /*
     * Phản hồi của nhà cung cấp đối với sản phẩm này.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "supplier_response_status",
            nullable = false,
            length = 30
    )
    private SupplierItemResponseStatus supplierResponseStatus =
            SupplierItemResponseStatus.PENDING;

    /*
     * Số lượng nhà cung cấp xác nhận có thể giao.
     */
    @Min(0)
    @Column(name = "confirmed_quantity")
    private Integer confirmedQuantity;

    /*
     * Giá mới do nhà cung cấp báo lại.
     *
     * Null nếu nhà cung cấp không thay đổi giá.
     */
    @Column(
            name = "confirmed_unit_price",
            precision = 18,
            scale = 2
    )
    private BigDecimal confirmedUnitPrice;

    /*
     * Ghi chú phản hồi đối với sản phẩm.
     *
     * Ví dụ:
     * - Chỉ còn 20 sản phẩm.
     * - Ngừng sản xuất từ tháng 7.
     * - Giá mới là 150.000 đồng.
     */
    @Lob
    @Column(
            name = "supplier_response_note",
            columnDefinition = "TEXT"
    )
    private String supplierResponseNote;

    @PrePersist
    protected void onCreate() {
        if (receivedQuantity == null) {
            receivedQuantity = 0;
        }
        if (supplierResponseStatus == null) {
            supplierResponseStatus = SupplierItemResponseStatus.PENDING;
        }
    }

    /*
     * Tính tổng tiền của dòng sản phẩm.
     *
     * Công thức:
     * orderedQuantity × unitPrice
     *
     * @Transient nghĩa là trường này không được tạo thành cột
     * trong database.
     */
    @Transient
    public BigDecimal getSubtotal() {
        if (orderedQuantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }

        return unitPrice.multiply(
                BigDecimal.valueOf(orderedQuantity)
        );
    }

    /*
     * Số lượng còn thiếu chưa được nhà cung cấp giao.
     */
    @Transient
    public Integer getRemainingQuantity() {
        if (orderedQuantity == null) {
            return 0;
        }

        int received = receivedQuantity == null
                ? 0
                : receivedQuantity;

        return Math.max(orderedQuantity - received, 0);
    }

    /*
     * Kiểm tra dòng sản phẩm đã được nhận đủ hay chưa.
     */
    @Transient
    public boolean isFullyReceived() {
        if (orderedQuantity == null || receivedQuantity == null) {
            return false;
        }

        return receivedQuantity >= orderedQuantity;
    }
    public BigDecimal getPriceDifference() {
        if (unitPrice == null || confirmedUnitPrice == null) {
            return BigDecimal.ZERO;
        }

        return confirmedUnitPrice.subtract(unitPrice);
    }
    public BigDecimal getPriceChangePercentage() {
        if (unitPrice == null
                || confirmedUnitPrice == null
                || unitPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return confirmedUnitPrice
                .subtract(unitPrice)
                .divide(unitPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    public boolean isPriceIncreased() {
        return unitPrice != null
                && confirmedUnitPrice != null
                && confirmedUnitPrice.compareTo(unitPrice) > 0;
    }
    public boolean isPriceDecreased() {
        return unitPrice != null
                && confirmedUnitPrice != null
                && confirmedUnitPrice.compareTo(unitPrice) < 0;
    }


}