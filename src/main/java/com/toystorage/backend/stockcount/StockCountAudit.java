package com.toystorage.backend.stockcount;

import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.entity.inventories.StockCountItems;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_count_audit", indexes = @Index(name = "idx_stock_count_audit_count", columnList = "stock_count_id"))
@Getter @Setter
public class StockCountAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCounts stockCount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private StockCountItems item;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Users actor;
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    @Column(name = "old_quantity")
    private Integer oldQuantity;
    @Column(name = "new_quantity")
    private Integer newQuantity;
    @Column(name = "reason", length = 1000)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
