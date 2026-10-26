package com.toystorage.backend.stockcount;

import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_count_notices", uniqueConstraints = @UniqueConstraint(name = "uk_sc_notice_plan_user", columnNames = {"stock_count_id", "user_id"}))
@Getter @Setter
public class StockCountNotice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCounts stockCount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "read_at")
    private LocalDateTime readAt;
}
