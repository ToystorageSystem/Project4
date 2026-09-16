package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chuyến giao hàng được phân công tài xế. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Deliveries delivery;

    /** Tài xế được phân công cho chuyến giao. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Users driver;

    /** Thời điểm tài xế được phân công. */
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    /** Thời điểm tài xế chấp nhận chuyến. */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /** Thời điểm tài xế từ chối chuyến. */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /** Lý do từ chối chuyến nếu có. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
