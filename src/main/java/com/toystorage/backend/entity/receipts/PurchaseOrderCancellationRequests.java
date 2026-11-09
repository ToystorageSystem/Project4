package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.PurchaseOrderCancellationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_cancellation_requests", indexes = {
        @Index(name = "idx_po_cancel_order", columnList = "purchase_order_id"),
        @Index(name = "idx_po_cancel_status", columnList = "status, requested_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderCancellationRequests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrders purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderCancellationStatus status;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "supplier_agreed", nullable = false)
    private Boolean supplierAgreed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private Users requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Users reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @PrePersist
    public void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (status == null) status = PurchaseOrderCancellationStatus.PENDING;
    }
}