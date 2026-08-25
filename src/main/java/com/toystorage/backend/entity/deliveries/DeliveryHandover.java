package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_handovers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_delivery_handover_delivery",
                        columnNames = "delivery_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "delivery_id",
            nullable = false
    )
    private Deliveries delivery;

    // Delivery Staff
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "handed_over_by",
            nullable = false
    )
    private Users handedOverBy;

    // Warehouse/Store Staff nhận kiện
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private Users receivedBy;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(
            name = "completed_at"
    )
    private LocalDateTime completedAt;

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