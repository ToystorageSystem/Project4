package com.toystorage.backend.entity.deliveries;

import com.toystorage.backend.entity.packages.Packages;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "delivery_packages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_delivery_packages_delivery_package",
                columnNames = {"shipment_id", "package_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPackages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chuyến giao hàng chứa kiện này. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Deliveries delivery;

    /** Kiện hàng được xếp vào chuyến giao. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private Packages packageEntity;

    /** Mã nội bộ của dòng liên kết. */
    @Column(name = "shipment_packages_code", nullable = false, length = 50)
    private String shipmentPackagesCode;
}
