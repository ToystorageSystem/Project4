package com.toystorage.backend.entity.receipts;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "receiving_incident_report_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_receiving_incident_item_discrepancy",
                        columnNames = "discrepancy_report_id"
                ),
                @UniqueConstraint(
                        name = "uk_receiving_incident_item_code",
                        columnNames = "receiving_incident_report_items_code"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivingIncidentReportItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiving_incident_report_items_code", nullable = false, unique = true, length = 50)
    private String receivingIncidentReportItemsCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ReceivingIncidentReports report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discrepancy_report_id", nullable = false)
    private DiscrepancyReports discrepancyReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private DiscrepancyType discrepancyType;

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;

    @Column(name = "damaged_quantity", nullable = false)
    private Integer damagedQuantity;

    @Column(name = "shortage_quantity", nullable = false)
    private Integer shortageQuantity;

    @Column(name = "surplus_quantity", nullable = false)
    private Integer surplusQuantity;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

}
