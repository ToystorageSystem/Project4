package com.toystorage.backend.entity.packages;

import com.toystorage.backend.entity.products.Products;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "package_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_package_items_package_product",
                columnNames = {"package_id", "product_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private Packages packageEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "package_items_code", nullable = false, length = 50)
    private String packageItemsCode;
}
