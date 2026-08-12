package com.toystorage.backend.entity.warehouses;


import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.warehouses.DamageType;
import com.toystorage.backend.enums.warehouses.DamagedGoodsItemStatus;
import com.toystorage.backend.enums.warehouses.DamageDisposition;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "damaged_goods_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamagedGoodsItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     Báo cáo hàng lỗi chứa sản phẩm này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "damaged_goods_report_id", nullable = false)
    private DamagedGoodsReports damagedGoodsReport;

    /**
     *Sản phẩm bị lỗi.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /**
     * Vị trí đang giữ hàng lỗi.
     *
     * Vị trí này nên có locationType là:
     * DAMAGED hoặc QUARANTINE.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocations location;

    /**
     * Số lượng sản phẩm bị lỗi.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Loại lỗi.
     *
     * BROKEN
     * TORN_PACKAGE
     * EXPIRED
     * CONTAMINATED
     * LEAKING
     * OTHER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false, length = 50)
    private DamageType damageType;

    /**
     * Mô tả chi tiết tình trạng sản phẩm.
     */
    @Lob
    @Column(name = "condition_note", columnDefinition = "TEXT")
    private String conditionNote;

    /**
     * Đường dẫn ảnh chứng minh hàng lỗi.
     */
    @Column(name = "evidence_image", length = 500)
    private String evidenceImage;

    /**
     * Hướng xử lý hàng lỗi.
     *
     * RETURN_TO_WAREHOUSE
     * HOLD
     * DESTROY
     * RETURN_TO_SUPPLIER
     * REPACK
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 40)
    private DamageDisposition disposition;

    /**
     * Trạng thái xử lý chi tiết hàng lỗi.
     *
     * RECORDED
     * APPROVED
     * TRANSFERRED
     * DESTROYED
     * RESOLVED
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private DamagedGoodsItemStatus status = DamagedGoodsItemStatus.REPORTED;
    /**
     * Mã nội bộ của chi tiết hàng lỗi.
     */
    @Column(
            name = "damaged_goods_items_code",
            nullable = false,
            length = 50
    )
    private String damagedGoodsItemsCode;

    @PrePersist
    protected void onCreate() {
        if (disposition == null) {
            disposition = DamageDisposition.RETURN_TO_WAREHOUSE;
        }

        if (status == null) {
            status = DamagedGoodsItemStatus.REPORTED;
        }
    }
}