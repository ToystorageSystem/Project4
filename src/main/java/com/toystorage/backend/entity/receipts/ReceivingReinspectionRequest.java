package com.toystorage.backend.entity.receipts;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "receiving_reinspection_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingReinspectionRequest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "goods_receipt_id",
            nullable = false
    )
    private GoodsReceipts goodsReceipt;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "requested_by",
            nullable = false
    )
    private Users requestedBy;

    @Column(
            name = "reason",
            nullable = false,
            length = 500
    )
    private String reason;

    @Column(
            name = "allow_same_staff",
            nullable = false
    )
    private Boolean allowSameStaff;

    @Column(
            name = "requested_at",
            nullable = false
    )
    private LocalDateTime requestedAt;
}