package com.toystorage.backend.entity.transfers;

import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(
        name = "stock_transfer_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_transfer_product",
                        columnNames = {"stock_transfer_id", "product_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @NotNull
    @Min(1)
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Min(0)
    @Builder.Default
    @Column(name = "approved_quantity", nullable = false)
    private Integer approvedQuantity = 0;

    @Min(0)
    @Builder.Default
    @Column(name = "packed_quantity", nullable = false)
    private Integer packedQuantity = 0;

    @Min(0)
    @Builder.Default
    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity = 0;

    @Min(0)
    @Builder.Default
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Min(0)
    @Builder.Default
    @Column(name = "shortage_quantity", nullable = false)
    private Integer shortageQuantity = 0;

    @Column(
            name = "picked_quantity",
            nullable = false
    )
    @Builder.Default
    private Integer pickedQuantity = 0;
    @Min(0)
    @Builder.Default
    @Column(name = "surplus_quantity", nullable = false)
    private Integer surplusQuantity = 0;

    @Column(
            name = "stock_transfer_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String stockTransferItemsCode;

    @PrePersist
    protected void onCreate() {
        if (approvedQuantity == null) approvedQuantity = 0;
        if (packedQuantity == null) packedQuantity = 0;
        if (shippedQuantity == null) shippedQuantity = 0;
        if (receivedQuantity == null) receivedQuantity = 0;
        if (shortageQuantity == null) shortageQuantity = 0;
        if (surplusQuantity == null) surplusQuantity = 0;
    }

    public void recalculateDiscrepancy() {
        int shipped = shippedQuantity == null ? 0 : shippedQuantity;
        int received = receivedQuantity == null ? 0 : receivedQuantity;
        shortageQuantity = Math.max(shipped - received, 0);
        surplusQuantity = Math.max(received - shipped, 0);
    }
}
