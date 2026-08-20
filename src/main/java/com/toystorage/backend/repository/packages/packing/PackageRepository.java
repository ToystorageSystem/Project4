package com.toystorage.backend.repository.packages.packing;

import com.toystorage.backend.entity.packages.Packages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageRepository
        extends JpaRepository<Packages, Long> {
    Optional<Packages>
    findByPackagesCode(
            String packagesCode
    );
}