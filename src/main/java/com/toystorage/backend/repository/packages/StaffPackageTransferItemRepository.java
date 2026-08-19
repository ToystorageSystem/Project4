package com.toystorage.backend.repository.packages;

import com.toystorage.backend.entity.packages.PackageTransferItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffPackageTransferItemRepository
        extends JpaRepository<PackageTransferItem, Long> {

    Optional<PackageTransferItem>
    findFirstByPackageEntityId(
            Long packageId
    );


    List<PackageTransferItem>
    findByStockTransferId(
            Long transferId
    );


    boolean existsByPackageEntityIdAndStockTransferId(
            Long packageId,
            Long transferId
    );
}