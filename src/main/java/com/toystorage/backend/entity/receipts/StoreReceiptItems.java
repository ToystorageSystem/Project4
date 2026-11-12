package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "store_receipt_items",

        /*
         * Một sản phẩm chỉ xuất hiện một lần
         * trong cùng một phiếu nhận hàng tại Store.
         *
         * Vì cần kiểm tra hai cột kết hợp:
         * store_receipt_id + product_id
         * nên sử dụng uniqueConstraints.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_receipt_product",
                        columnNames = {
                                "store_receipt_id",
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
public class StoreReceiptItems {

    /*
     * Khóa chính của dòng sản phẩm.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Phiếu nhận hàng chứa dòng sản phẩm này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_receipt_id",
            nullable = false
    )
    private StoreReceipts storeReceipt;

    /*
     * Sản phẩm Store nhận.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Products product;

    /*
     * Số lượng dự kiến Store phải nhận.
     *
     * Giá trị này lấy từ Stock Transfer hoặc Package.
     */
    @Column(
            name = "expected_quantity",
            nullable = false
    )
    private Integer expectedQuantity;

    /*
     * Số lượng Store thực tế kiểm đếm được.
     */
    @Builder.Default
    @Column(
            name = "actual_quantity",
            nullable = false
    )
    private Integer actualQuantity = 0;

    /*
     * Số lượng bị thiếu.
     *
     * shortageQuantity =
     * max(expectedQuantity - actualQuantity, 0)
     */
    @Builder.Default
    @Column(
            name = "shortage_quantity",
            nullable = false
    )
    private Integer shortageQuantity = 0;

    /*
     * Số lượng bị dư.
     *
     * surplusQuantity =
     * max(actualQuantity - expectedQuantity, 0)
     */
    @Builder.Default
    @Column(
            name = "surplus_quantity",
            nullable = false
    )
    private Integer surplusQuantity = 0;

    /*
     * Số lượng phát hiện bị hư hỏng.
     */
    @Builder.Default
    @Column(
            name = "damaged_quantity",
            nullable = false
    )
    private Integer damagedQuantity = 0;

    /*
     * Mã nội bộ của dòng nhận hàng.
     */
    @Column(
            name = "store_receipt_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String storeReceiptItemsCode;

    @PrePersist
    protected void onCreate() {
        if (actualQuantity == null) {
            actualQuantity = 0;
        }

        if (shortageQuantity == null) {
            shortageQuantity = 0;
        }

        if (surplusQuantity == null) {
            surplusQuantity = 0;
        }

        if (damagedQuantity == null) {
            damagedQuantity = 0;
        }
    }

    /*
     * Tính tự động chênh lệch số lượng.
     *
     * Có thể gọi phương thức này sau khi nhập actualQuantity.
     */
    public void calculateDifference() {
        if (expectedQuantity == null || actualQuantity == null) {
            shortageQuantity = 0;
            surplusQuantity = 0;
            return;
        }

        if (actualQuantity < expectedQuantity) {
            shortageQuantity = expectedQuantity - actualQuantity;
            surplusQuantity = 0;
        } else {
            surplusQuantity = actualQuantity - expectedQuantity;
            shortageQuantity = 0;
        }
    }

    /*
     * Số lượng hàng đạt có thể nhập vào tồn Store.
     *
     * Không cộng hàng bị hư hỏng.
     */
    @Transient
    public Integer getAcceptedQuantity() {
        if (actualQuantity == null) {
            return 0;
        }

        int damaged = damagedQuantity == null
                ? 0
                : damagedQuantity;

        return Math.max(actualQuantity - damaged, 0);
    }

    /*
     * Kiểm tra Store đã nhận đủ và không có hàng lỗi.
     */
    @Transient
    public boolean isMatched() {
        return expectedQuantity != null
                && actualQuantity != null
                && expectedQuantity.equals(actualQuantity)
                && (damagedQuantity == null || damagedQuantity == 0);
    }
}