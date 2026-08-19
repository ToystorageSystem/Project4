package com.toystorage.backend.entity.stores;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.WarehouseLocations;
import com.toystorage.backend.enums.stores.ReturnItemCondition;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "store_return_items",

        /*
         * Một sản phẩm tại cùng một vị trí chỉ nên xuất hiện
         * một lần trong cùng phiếu trả hàng.
         *
         * Cần kiểm tra ba cột kết hợp nên dùng uniqueConstraints.
         */
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

    /*
     * Khóa chính của dòng hàng trả.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Phiếu trả hàng chứa dòng sản phẩm này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_return_id",
            nullable = false
    )
    private StoreReturns storeReturn;

    /*
     * Sản phẩm cần trả về kho.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Products product;

    @Column(
            name = "evidence_image_url",
            length = 500
    )
    private String evidenceImageUrl;

    /*
     * Vị trí tại Store đang chứa sản phẩm.
     *
     * Một sản phẩm có thể nằm trên nhiều kệ,
     * vì vậy cần biết chính xác xuất từ vị trí nào.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "from_location_id",
            nullable = false
    )
    private WarehouseLocations fromLocation;

    /*
     * Số lượng Store đề nghị trả.
     */
    @Column(
            name = "requested_quantity",
            nullable = false
    )
    private Integer requestedQuantity;

    /*
     * Số lượng Business hoặc Warehouse đồng ý cho trả.
     *
     * Khi phiếu được APPROVED,
     * Store sẽ bị trừ theo approvedQuantity.
     */
    @Builder.Default
    @Column(
            name = "approved_quantity",
            nullable = false
    )
    private Integer approvedQuantity = 0;

    /*
     * Số lượng Store thực tế đã xuất.
     */
    @Builder.Default
    @Column(
            name = "issued_quantity",
            nullable = false
    )
    private Integer issuedQuantity = 0;

    /*
     * Số lượng kho thực tế đã nhận.
     */
    @Builder.Default
    @Column(
            name = "received_quantity",
            nullable = false
    )
    private Integer receivedQuantity = 0;

    /*
     * Số lượng kho từ chối nhận.
     *
     * Ví dụ:
     * - Không đúng sản phẩm.
     * - Số lượng không đúng.
     * - Hàng không thuộc phiếu trả.
     */
    @Builder.Default
    @Column(
            name = "rejected_quantity",
            nullable = false
    )
    private Integer rejectedQuantity = 0;

    /*
     * Tình trạng hàng trả.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            name = "condition_status",
            nullable = false,
            length = 30
    )
    private ReturnItemCondition conditionStatus =
            ReturnItemCondition.NORMAL;

    /*
     * Ghi chú cho từng sản phẩm.
     */
    @Lob
    @Column(
            name = "note",
            columnDefinition = "TEXT"
    )
    private String note;

    /*
     * Mã nội bộ của dòng sản phẩm trả.
     */
    @Column(
            name = "store_return_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String storeReturnItemsCode;

    @PrePersist
    protected void onCreate() {
        if (approvedQuantity == null) {
            approvedQuantity = 0;
        }

        if (issuedQuantity == null) {
            issuedQuantity = 0;
        }

        if (receivedQuantity == null) {
            receivedQuantity = 0;
        }

        if (rejectedQuantity == null) {
            rejectedQuantity = 0;
        }

        if (conditionStatus == null) {
            conditionStatus = ReturnItemCondition.NORMAL;
        }
    }

    /*
     * Số lượng kho có thể nhập lại vào tồn.
     *
     * Với hàng NORMAL có thể nhập kệ thường.
     * Hàng DAMAGED/EXPIRED/QUARANTINE phải nhập
     * vị trí chuyên biệt, không nhập chung hàng bán.
     */
    @Transient
    public Integer getAcceptedQuantity() {
        if (receivedQuantity == null) {
            return 0;
        }

        int rejected = rejectedQuantity == null
                ? 0
                : rejectedQuantity;

        return Math.max(receivedQuantity - rejected, 0);
    }
}