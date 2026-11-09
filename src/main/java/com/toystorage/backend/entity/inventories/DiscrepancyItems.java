package com.toystorage.backend.entity.inventories;

import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discrepancy_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Báo cáo chênh lệch chứa dòng sản phẩm này. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discrepancy_report_id", nullable = false)
    private DiscrepancyReports discrepancyReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    /** Số lượng hệ thống/chứng từ mong đợi. */
    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    /** Số lượng thực tế kiểm nhận/kiểm đếm. */
    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    /** actualQuantity - expectedQuantity. */
    @Column(name = "difference_quantity", nullable = false)
    private Integer differenceQuantity;

    @Column(name = "discrepancy_items_code", nullable = false, length = 50)
    private String discrepancyItemsCode;
}
