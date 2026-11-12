package com.toystorage.backend.entity.shipments;

import com.toystorage.backend.entity.transfers.StockTransfer;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(
        name = "shipment_manifest_transfers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_manifest_transfer",
                        columnNames = {
                                "manifest_id",
                                "transfer_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentManifestTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manifest_id", nullable = false)
    private ShipmentManifests manifest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;
}
