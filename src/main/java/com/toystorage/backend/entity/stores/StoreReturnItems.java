package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.stores.ReturnItemCondition;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "store_return_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_return_product_location",
                        columnNames = {
                                "store_return_id",
                                "product_id",
                                "from_location_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReturnItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_return_id", nullable = false)
    private StoreReturns storeReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "evidence_image_url", length = 500)
    private String evidenceImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_location_id", nullable = false)
    private WarehouseLocations fromLocation;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Builder.Default
    @Column(name = "approved_quantity", nullable = false)
    private Integer approvedQuantity = 0;

    @Builder.Default
    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity = 0;

    @Builder.Default
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Builder.Default
    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "condition_status", nullable = false, length = 30)
    private ReturnItemCondition conditionStatus = ReturnItemCondition.NORMAL;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @Column(name = "inspected", nullable = false)
    private Boolean inspected = false;

    /*
     * Prevent posting the same Store Return item to inventory twice.
     * Needed because MINOR items can be posted before all MAJOR
     * discrepancies of the same return are resolved.
     */
    @Builder.Default
    @Column(name = "inventory_posted", nullable = false)
    private Boolean inventoryPosted = false;

    @Column(
            name = "store_return_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String storeReturnItemsCode;

    @PrePersist
    protected void onCreate() {
        if (approvedQuantity == null) approvedQuantity = 0;
        if (issuedQuantity == null) issuedQuantity = 0;
        if (receivedQuantity == null) receivedQuantity = 0;
        if (rejectedQuantity == null) rejectedQuantity = 0;
        if (conditionStatus == null) conditionStatus = ReturnItemCondition.NORMAL;
        if (inspected == null) inspected = false;
        if (inventoryPosted == null) inventoryPosted = false;
    }

    @Transient
    public Integer getAcceptedQuantity() {
        if (receivedQuantity == null) {
            return 0;
        }

        int rejected = rejectedQuantity == null ? 0 : rejectedQuantity;
        return Math.max(receivedQuantity - rejected, 0);
    }
}
