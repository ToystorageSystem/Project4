package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.RolePermissions;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository
        extends JpaRepository<RolePermissions, Long> {


    // =========================================================
    // GET PERMISSIONS OF ROLE
    // =========================================================

    List<RolePermissions>
    findByRole_IdOrderByPermission_IdAsc(
            Long roleId
    );


    // =========================================================
    // CHECK ROLE - PERMISSION
    // =========================================================

    boolean existsByRole_IdAndPermission_Id(
            Long roleId,
            Long permissionId
    );


    // =========================================================
    // FIND ROLE - PERMISSION
    // =========================================================

    Optional<RolePermissions>
    findByRole_IdAndPermission_Id(
            Long roleId,
            Long permissionId
    );


    // =========================================================
    // CHECK ROLE HAS PERMISSIONS
    // =========================================================

    /*
     * DELETE ROLE đang sử dụng.
     */
    boolean existsByRole_Id(
            Long roleId
    );


    // =========================================================
    // FIND ROLES USING PERMISSION
    // =========================================================

    /*
     * Dùng khi Permission thay đổi trạng thái.
     *
     * Ví dụ:
     *
     * USER_VIEW -> INACTIVE
     *
     * Ta tìm tất cả Role đang có USER_VIEW,
     * sau đó tìm User của các Role đó
     * để expire session.
     */
    List<RolePermissions> findByPermission_Id(
            Long permissionId
    );
}