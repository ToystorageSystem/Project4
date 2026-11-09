package com.toystorage.backend.entity.stores;

import java.time.LocalDateTime;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.receipts.StoreReceipts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.stores.StockTransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** History of every store stock change (audit). Insert only – never updated. */
@Entity
@Table(name = "store_stock_transactions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreStockTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_stock_transactions_code", nullable = false, length = 50)
    private String code;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private StockTransactionType type;

    @Column(name = "shelf_change", nullable = false)
    private int shelfChange;

    @Column(name = "backroom_change", nullable = false)
    private int backroomChange;

    @Column(name = "shelf_after", nullable = false)
    private int shelfAfter;

    @Column(name = "backroom_after", nullable = false)
    private int backroomAfter;

    /** store_sales.id (phase 3) – plain id, no entity yet */
    @Column(name = "sale_id")
    private Long saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_receipt_id")
    private StoreReceipts storeReceipt;

    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}