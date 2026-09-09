package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.Permissions;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository
        extends JpaRepository<Permissions, Long> {


    // =========================================================
    // FIND BY PERMISSION CODE
    // =========================================================

    Optional<Permissions> findByPermissionCodeIgnoreCase(
            String permissionCode
    );


    // =========================================================
    // CHECK DUPLICATE PERMISSION CODE
    // =========================================================

    boolean existsByPermissionCodeIgnoreCase(
            String permissionCode
    );
}