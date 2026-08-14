package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.InspectionResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptInspections {

    /*
     * Khóa chính của bản ghi kiểm hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Phiếu nhập hàng được kiểm tra.
     *
     * Một Goods Receipt có thể có nhiều dòng kiểm tra.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "goods_receipt_id",
            nullable = false
    )
    private GoodsReceipts goodsReceipt;

    /*
     * Mã kiện hoặc mã thùng được kiểm tra.
     *
     * Có thể null nếu hàng không được giao theo kiện.
     */
    @Column(
            name = "package_code",
            length = 100
    )
    private String packageCode;

    /*
     * Sản phẩm đang được kiểm tra.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Products product;

    /*
     * Số lượng theo Purchase Order hoặc chứng từ giao hàng.
     */
    @Column(
            name = "expected_quantity",
            nullable = false
    )
    private Integer expectedQuantity;

    /*
     * Số lượng thực tế nhân viên kho đếm được.
     */
    @Column(
            name = "actual_quantity",
            nullable = false
    )
    private Integer actualQuantity;

    @Column(name = "evidence_image", length = 500)
    private String evidenceImage;
    /*
     * Kết quả kiểm tra.
     *
     * MATCHED:
     * Số lượng và tình trạng đúng.
     *
     * SHORTAGE:
     * Thiếu hàng.
     *
     * SURPLUS:
     * Dư hàng.
     *
     * DAMAGED:
     * Có hàng hư hỏng.
     *
     * PARTIAL:
     * Có nhiều tình trạng kết hợp.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "inspected_result",
            nullable = false,
            length = 30
    )
    private InspectionResult inspectedResult;

    /*
     * Nhân viên kho thực hiện kiểm hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inspected_by",
            nullable = false
    )
    private Users inspectedBy;

    /*
     * Thời điểm kiểm hàng.
     */
    @Column(
            name = "inspected_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime inspectedAt;

    /*
     * Ghi chú về kết quả kiểm tra.
     *
     * Ví dụ:
     * - Seal còn nguyên.
     * - Thùng bị móp.
     * - Thiếu 5 sản phẩm.
     */
    @Lob
    @Column(
            name = "notes",
            columnDefinition = "TEXT"
    )
    private String notes;

    /*
     * Mã nội bộ của bản ghi kiểm tra.
     */
    @Column(
            name = "receipt_inspections_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String receiptInspectionsCode;

    @PrePersist
    protected void onCreate() {
        if (inspectedAt == null) {
            inspectedAt = LocalDateTime.now();
        }
    }

    /*
     * Tính chênh lệch giữa số lượng thực tế và dự kiến.
     *
     * Kết quả:
     * > 0: dư hàng
     * < 0: thiếu hàng
     * = 0: đủ hàng
     *
     * @Transient nghĩa là không tạo cột trong database.
     */
    @Transient
    public Integer getDifferenceQuantity() {
        if (expectedQuantity == null || actualQuantity == null) {
            return 0;
        }

        return actualQuantity - expectedQuantity;
    }
}