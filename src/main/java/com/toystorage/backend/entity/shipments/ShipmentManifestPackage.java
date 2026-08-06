package com.toystorage.backend.entity.shipments;

import com.toystorage.backend.entity.packages.Packages;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "shipment_manifest_packages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_manifest_package",
                        columnNames = {
                                "manifest_id",
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
public class ShipmentManifestPackage {

    /*
     * Khóa chính của bảng liên kết.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Bảng kê đi hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "manifest_id",
            nullable = false
    )
    private ShipmentManifests manifest;

    /*
     * Kiện hàng thuộc bảng kê.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "package_id",
            nullable = false
    )
    private Packages packageEntity;
}