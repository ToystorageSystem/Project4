package com.toystorage.backend.entity.stores;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A simulated sale at the store. Insert only – no cancel / refund. */
@Entity
@Table(name = "store_sales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreSales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_sales_code", nullable = false, length = 50)
    private String code;

    /** = stores.warehouse_id */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** saved together with the sale (cascade) */
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<StoreSaleItems> items = new ArrayList<>();

    public static StoreSales create(String code, Long storeId, String note, Users createdBy) {
        StoreSales sale = new StoreSales();
        sale.code = code;
        sale.storeId = storeId;
        sale.note = note;
        sale.createdBy = createdBy;
        sale.createdAt = LocalDateTime.now();
        sale.totalQuantity = 0;
        sale.totalAmount = BigDecimal.ZERO;
        return sale;
    }

    /** adds a line and keeps the totals in sync */
    public StoreSaleItems addItem(Products product, int quantity, BigDecimal unitPrice, int fromShelf) {
        StoreSaleItems item = StoreSaleItems.of(this, product, quantity, unitPrice, fromShelf);
        items.add(item);
        totalQuantity += quantity;
        totalAmount = totalAmount.add(item.getLineTotal());
        return item;
    }
}