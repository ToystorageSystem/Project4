package com.toystorage.backend.repository.auth;

import com.toystorage.backend.entity.auth.Roles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository
        extends JpaRepository<Roles, Long> {


    // =========================================================
    // FIND BY ROLE CODE
    // =========================================================

    Optional<Roles> findByRoleCode(
            String roleCode
    );


    // =========================================================
    // CHECK ROLE CODE EXISTS
    // =========================================================

    boolean existsByRoleCode(
            String roleCode
    );


    // =========================================================
    // CHECK ROLE NAME EXISTS
    // =========================================================

    boolean existsByRoleNameIgnoreCase(
            String roleName
    );


    // =========================================================
    // CHECK ROLE NAME EXISTS EXCEPT CURRENT ROLE
    // =========================================================

    boolean existsByRoleNameIgnoreCaseAndIdNot(
            String roleName,
            Long roleId
    );


    // =========================================================
    // SEARCH ROLE
    // =========================================================

    /*
     * Tìm theo:
     *
     * - roleCode
     * - roleName
     * - description
     *
     * Không phân biệt chữ hoa / chữ thường.
     */
    @Query("""
        SELECT r
        FROM Roles r
        WHERE LOWER(r.roleCode)
                LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(r.roleName)
                LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(COALESCE(r.description, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY r.id ASC
    """)
    List<Roles> searchRoles(
            @Param("search")
            String search
    );
}