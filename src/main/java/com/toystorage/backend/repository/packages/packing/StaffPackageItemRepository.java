package com.toystorage.backend.repository.packages.packing;

import com.toystorage.backend.entity.packages.PackageItems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffPackageItemRepository
        extends JpaRepository<PackageItems, Long> {

    List<PackageItems>
    findByPackageEntityId(
            Long packageId
    );


    Optional<PackageItems>
    findByPackageEntityIdAndProductId(
            Long packageId,
            Long productId
    );


    @Query("""
        select coalesce(sum(pi.quantity), 0)
        from PackageItems pi
        join PackageTransferItem pti
            on pti.packageEntity.id = pi.packageEntity.id
        where pti.stockTransfer.id = :transferId
          and pi.product.id = :productId
    """)
    Long sumPackedQuantity(
            @Param("transferId")
            Long transferId,

            @Param("productId")
            Long productId
    );
}