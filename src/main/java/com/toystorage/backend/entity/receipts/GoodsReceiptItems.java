package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "goods_receipt_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_goods_receipt_items_receipt_product",
                columnNames = {"goods_receipt_id", "product_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Phiếu nhập kho chứa dòng sản phẩm. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipts goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;

    @Column(name = "damaged_quantity", nullable = false)
    private Integer damagedQuantity;

    @Column(name = "surplus_quantity", nullable = false)
    private Integer surplusQuantity;

    @Column(name = "shortage_quantity", nullable = false)
    private Integer shortageQuantity;

    @Column(name = "goods_receipt_items_code", nullable = false, length = 50)
    private String goodsReceiptItemsCode;
}
