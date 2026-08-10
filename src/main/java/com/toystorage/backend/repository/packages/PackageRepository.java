package com.toystorage.backend.repository.packages;

import com.toystorage.backend.entity.packages.Packages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageRepository
        extends JpaRepository<Packages, Long> {
}