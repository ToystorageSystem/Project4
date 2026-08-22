package com.toystorage.backend.entity.transfers;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.transfers.TransferReasonCode;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "stock_transfers",
        indexes = {
                @Index(name = "idx_stock_transfers_from_warehouse", columnList = "from_warehouse_id"),
                @Index(name = "idx_stock_transfers_to_warehouse", columnList = "to_warehouse_id"),
                @Index(name = "idx_stock_transfers_status", columnList = "status"),
                @Index(name = "idx_stock_transfers_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 50)
    private String transferCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_warehouse_id", nullable = false)
    private Warehouses fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    private Warehouses toWarehouse;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "transfer_type", nullable = false, length = 30)
    private TransferType transferType = TransferType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private TransferReasonCode reasonCode;

    @Lob
    @Column(name = "reason_note", columnDefinition = "TEXT")
    private String reasonNote;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 40)
    private TransferStatus status = TransferStatus.DRAFT;

    @OneToMany(
            mappedBy = "stockTransfer",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @Builder.Default
    private List<StockTransferItems> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packing_completed_by")
    private Users packingCompletedBy;

    @Column(name = "packing_completed_at")
    private LocalDateTime packingCompletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Users confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Lob
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    @Column(name = "expected_shipment_date")
    private LocalDate expectedShipmentDate;

    @Column(name = "expected_receipt_date")
    private LocalDate expectedReceiptDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "stock_transfers_code", nullable = false, unique = true, length = 50)
    private String stockTransfersCode;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(StockTransferItems item) {
        if (item == null) return;
        items.add(item);
        item.setStockTransfer(this);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = TransferStatus.DRAFT;
        if (transferType == null) transferType = TransferType.NORMAL;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
