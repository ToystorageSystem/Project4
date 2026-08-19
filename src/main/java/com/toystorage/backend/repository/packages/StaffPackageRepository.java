package com.toystorage.backend.repository.packages;

import com.toystorage.backend.entity.packages.Packages;
import com.toystorage.backend.enums.packages.PackageStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffPackageRepository
        extends JpaRepository<Packages, Long> {

    boolean existsByPackagesCode(
            String packagesCode
    );

    Optional<Packages>
    findByPackagesCode(
            String packagesCode
    );

    List<Packages>
    findByFromWarehouseIdAndStatusOrderByCreatedAtDesc(
            Long warehouseId,
            PackageStatus status
    );
}
