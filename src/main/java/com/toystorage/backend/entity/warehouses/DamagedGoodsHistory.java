package com.toystorage.backend.entity.warehouses;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.warehouses.DamagedGoodsHistoryAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "damaged_goods_history",
        indexes = {
                @Index(
                        name = "idx_damage_history_report",
                        columnList = "report_id"
                ),
                @Index(
                        name = "idx_damage_history_item",
                        columnList = "item_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamagedGoodsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "report_id",
            nullable = false
    )
    private DamagedGoodsReports report;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private DamagedGoodsItems item;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            length = 50
    )
    private DamagedGoodsHistoryAction action;


    @Column(name = "quantity")
    private Integer quantity;


    @Lob
    @Column(
            name = "note",
            columnDefinition = "TEXT"
    )
    private String note;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "performed_by",
            nullable = false
    )
    private Users performedBy;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}