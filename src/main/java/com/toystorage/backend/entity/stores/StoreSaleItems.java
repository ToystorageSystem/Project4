package com.toystorage.backend.entity.stores;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.toystorage.backend.entity.products.Products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One product line of a sale. from_shelf + from_backroom = quantity (DB CHECK). */
@Entity
@Table(name = "store_sale_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreSaleItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private StoreSales sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** selling price at the time of sale (snapshot) */
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "from_shelf", nullable = false)
    private int fromShelf;

    @Column(name = "from_backroom", nullable = false)
    private int fromBackroom;

    static StoreSaleItems of(StoreSales sale, Products product, int quantity,
                             BigDecimal unitPrice, int fromShelf) {
        StoreSaleItems item = new StoreSaleItems();
        item.sale = sale;
        item.product = product;
        item.quantity = quantity;
        item.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        item.lineTotal = item.unitPrice.multiply(BigDecimal.valueOf(quantity));
        item.fromShelf = fromShelf;
        item.fromBackroom = quantity - fromShelf;
        return item;
    }
}