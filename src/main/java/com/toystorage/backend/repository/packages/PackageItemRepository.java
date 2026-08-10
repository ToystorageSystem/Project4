package com.toystorage.backend.repository.packages;

import com.toystorage.backend.entity.packages.PackageItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageItemRepository
        extends JpaRepository<PackageItems, Long> {

    List<PackageItems> findByPackageEntityId(Long packageId);
}
