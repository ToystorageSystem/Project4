package com.toystorage.backend.repository.packages.packing;

import com.toystorage.backend.entity.packages.PackageItems;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageItemRepository
        extends JpaRepository<PackageItems, Long> {

    /**
     * Task #13:
     * lấy toàn bộ sản phẩm trong một kiện hàng.
     *
     * Fetch luôn product để map:
     * code, barcode, name, base unit và quantity.
     */
    @EntityGraph(attributePaths = {
            "product"
    })
    List<PackageItems> findByPackageEntityId(
            Long packageId
    );
}