package com.toystorage.backend.repository.packages;


import com.toystorage.backend.entity.packages.PackageTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageTransferItemRepository
        extends JpaRepository<PackageTransferItem, Long> {

    List<PackageTransferItem>
    findByStockTransferId(Long stockTransferId);
}