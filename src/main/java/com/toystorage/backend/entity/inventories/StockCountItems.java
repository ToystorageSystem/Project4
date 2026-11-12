package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_count_items", uniqueConstraints = @UniqueConstraint(name = "uk_stock_count_product_location", columnNames = {"stock_count_id", "product_id", "location_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockCountItems {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCounts stockCount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocations location;
    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;
    @Column(name = "first_count_quantity")
    private Integer firstCountQuantity;
    @Column(name = "second_count_quantity")
    private Integer secondCountQuantity;
    @Column(name = "final_quantity")
    private Integer finalQuantity;
    @Column(name = "difference_quantity")
    private Integer differenceQuantity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counted_by")
    private Users countedBy;
    @Column(name = "corrected_quantity")
    private Integer correctedQuantity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by")
    private Users correctedBy;
    @Column(name = "corrected_at")
    private LocalDateTime correctedAt;
    @Lob @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recounted_by")
    private Users recountedBy;
    @Column(name = "stock_count_items_code", nullable = false, length = 50)
    private String stockCountItemsCode;
    @Builder.Default @Column(name = "recount_requested")
    private Boolean recountRequested = false;
    public void calculateDifference() {
        differenceQuantity = finalQuantity == null || systemQuantity == null
                ? null : finalQuantity - systemQuantity;
    }
}
