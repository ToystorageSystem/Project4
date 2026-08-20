package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_assignment_history",
        indexes = {

                @Index(
                        name = "idx_delivery_assignment_delivery",
                        columnList = "delivery_id"
                ),

                @Index(
                        name = "idx_delivery_assignment_driver",
                        columnList = "driver_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "delivery_id",
            nullable = false
    )
    private Deliveries delivery;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "driver_id",
            nullable = false
    )
    private Users driver;


    @Column(
            name = "assigned_at",
            nullable = false
    )
    private LocalDateTime assignedAt;


    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;


    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;


    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;


    @PrePersist
    protected void onCreate() {

        if (assignedAt == null) {

            assignedAt =
                    LocalDateTime.now();
        }
    }
}