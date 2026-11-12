package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.warehouses.LowStockSeverity;
import jakarta.persistence.*;
import lombok.*;

/** Dong san pham trong bao cao sap het hang; luu snapshot so luong tai thoi diem gui. */
@Entity
@Table(name = "low_stock_report_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockReportItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LowStockReport report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "on_hand", nullable = false)
    private Integer onHand;

    @Column(name = "reserved", nullable = false)
    private Integer reserved;

    @Column(name = "available", nullable = false)
    private Integer available;

    @Column(name = "minimum_level", nullable = false)
    private Integer minimumLevel;

    @Column(name = "suggested_quantity", nullable = false)
    private Integer suggestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private LowStockSeverity severity;

    /** 0 = het hang, 1 = nguy cap, 2 = thap; dung de sap xep. */
    @Column(name = "severity_rank", nullable = false)
    private Integer severityRank;
}
