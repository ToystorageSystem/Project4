package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "stock_count_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_count_product_location",
                        columnNames = {"stock_count_id", "product_id", "location_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCountItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Phiếu kiểm kê chứa dòng dữ liệu này. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCounts stockCount;

    /* Sản phẩm được kiểm kê. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /* Kệ/vị trí được kiểm kê. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocations location;

    /* Số lượng hệ thống ghi nhận trước khi đếm. */
    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    /* Kết quả đếm lần đầu. */
    @Column(name = "first_count_quantity")
    private Integer firstCountQuantity;

    /* Kết quả đếm lại khi lần đầu có chênh lệch. */
    @Column(name = "second_count_quantity")
    private Integer secondCountQuantity;

    /* Số lượng cuối cùng được xác nhận. */
    @Column(name = "final_quantity")
    private Integer finalQuantity;

    /* Chênh lệch cuối cùng = finalQuantity - systemQuantity. */
    @Column(name = "difference_quantity")
    private Integer differenceQuantity;

    /* Người thực hiện lần đếm đầu tiên. */
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

    @Lob
    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;
    /* Người thực hiện lần đếm lại. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recounted_by")
    private Users recountedBy;

    /* Mã nội bộ theo đúng database. */
    @Column(name = "stock_count_items_code", nullable = false, length = 50)
    private String stockCountItemsCode;

    /* Cập nhật số lượng cuối cùng và chênh lệch. */
    public void calculateDifference() {
        if (finalQuantity == null || systemQuantity == null) {
            differenceQuantity = null;
            return;
        }
        differenceQuantity = finalQuantity - systemQuantity;
    }
}
