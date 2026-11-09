package com.toystorage.backend.entity.packages;

import com.toystorage.backend.entity.transfers.StockTransfer;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "package_transfer_items",

        /*
         * Một package chỉ được liên kết một lần
         * với cùng một stock transfer.
         *
         * Vì cần kiểm tra tính duy nhất của 2 cột kết hợp:
         * package_id + stock_transfer_id
         * nên dùng uniqueConstraints.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_package_transfer",
                        columnNames = {
                                "package_id",
                                "stock_transfer_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageTransferItem {

    /*
     * Khóa chính của bản ghi liên kết.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Kiện hàng được liên kết.
     *
     * Một Package có thể liên kết với nhiều Stock Transfer
     * nếu hệ thống cho phép một kiện chứa hàng của nhiều phiếu điều chuyển.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "package_id",
            nullable = false
    )
    private Packages packageEntity;

    /*
     * Phiếu điều chuyển được liên kết với kiện hàng.
     *
     * Một Stock Transfer có thể được chia thành nhiều Package.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stock_transfer_id",
            nullable = false
    )
    private StockTransfer stockTransfer;

    /*
     * Mã nội bộ của bản ghi liên kết.
     *
     * Ví dụ:
     * PTI2026080001
     */
    @Column(
            name = "package_transfer_items_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String packageTransferItemsCode;
}