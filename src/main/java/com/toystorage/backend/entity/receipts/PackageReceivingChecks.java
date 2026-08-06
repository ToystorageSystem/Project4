package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.receipts.InspectionMethod;
import com.toystorage.backend.enums.receipts.PackageCondition;
import com.toystorage.backend.enums.receipts.SealStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "package_receiving_checks",

        /*
         * Một kiện chỉ được kiểm tra một lần
         * trong cùng một phiếu nhận hàng tại Store.
         *
         * Đây là unique kết hợp hai cột:
         * store_receipt_id + package_id.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_receipt_package",
                        columnNames = {
                                "store_receipt_id",
                                "package_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageReceivingChecks {

    /*
     * Khóa chính của bản ghi kiểm tra kiện hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Phiếu nhận hàng tại Store.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "store_receipt_id",
            nullable = false
    )
    private StoreReceipts storeReceipt;

    /*
     * Kiện hàng được Store kiểm tra.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "package_id",
            nullable = false
    )
    private Packages packageEntity;

    /*
     * Tình trạng seal của kiện hàng.
     *
     * INTACT:
     * Seal còn nguyên.
     *
     * BROKEN:
     * Seal bị rách hoặc bị mở.
     *
     * MISSING:
     * Không có seal.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "seal_status",
            nullable = false,
            length = 30
    )
    private SealStatus sealStatus;

    /*
     * Tình trạng bên ngoài của kiện hàng.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "package_condition",
            nullable = false,
            length = 30
    )
    private PackageCondition packageCondition;

    /*
     * Phương pháp Store dùng để kiểm tra kiện.
     *
     * FULL_COUNT:
     * Đếm toàn bộ sản phẩm.
     *
     * SAMPLE_CHECK:
     * Kiểm tra theo mẫu.
     *
     * SEAL_ONLY:
     * Chỉ kiểm tra seal và tình trạng kiện.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "inspection_method",
            nullable = false,
            length = 30
    )
    private InspectionMethod inspectionMethod;

    /*
     * Nhân viên Store thực hiện kiểm tra kiện hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "checked_by",
            nullable = false
    )
    private Users checkedBy;

    /*
     * Thời điểm kiểm tra.
     */
    @Column(
            name = "checked_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime checkedAt;

    /*
     * Ghi chú của nhân viên kiểm hàng.
     *
     * Ví dụ:
     * - Seal nguyên nhưng thùng bị móp.
     * - Có dấu hiệu mở kiện.
     * - Đã kiểm toàn bộ sản phẩm.
     */
    @Lob
    @Column(
            name = "notes",
            columnDefinition = "TEXT"
    )
    private String notes;

    /*
     * Mã nội bộ của bản ghi kiểm tra kiện hàng.
     */
    @Column(
            name = "package_receiving_checks_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String packageReceivingChecksCode;

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now();
        }
    }
}