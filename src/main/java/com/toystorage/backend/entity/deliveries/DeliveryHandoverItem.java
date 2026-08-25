package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.enums.deliveries.HandoverPackageCondition;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_handover_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_handover_package",
                        columnNames = {
                                "handover_id",
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
public class DeliveryHandoverItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "handover_id",
            nullable = false
    )
    private DeliveryHandover handover;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "package_id",
            nullable = false
    )
    private Packages packageEntity;

    @Column(
            name = "actual_seal_number",
            length = 100
    )
    private String actualSealNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "condition_status",
            nullable = false,
            length = 30
    )
    private HandoverPackageCondition condition;

    @Column(
            name = "issue_note",
            length = 500
    )
    private String issueNote;

    @Column(
            name = "scanned_at"
    )
    private LocalDateTime scannedAt;
}