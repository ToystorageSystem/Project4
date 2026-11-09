package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.auth.UserRoles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import com.toystorage.backend.enums.users.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository
        extends JpaRepository<Users, Long> {


    Optional<Users> findByEmailIgnoreCase(String email);

    Optional<Users> findByEmail(
            String email
    );


    boolean existsByEmail(
            String email
    );


    // =====================================================
    // ACTIVE USER BY ROLE CODE
    // =====================================================

    @Query(
            value = """
                SELECT DISTINCT u.*
                FROM users u
                JOIN user_roles ur
                  ON ur.user_id = u.id
                JOIN roles r
                  ON r.id = ur.role_id
                WHERE r.roles_code = :roleCode
                  AND u.status = 'ACTIVE'
            """,
            nativeQuery = true
    )
    List<Users> findActiveUsersByRoleCode(
            @Param("roleCode")
            String roleCode
    );
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByIdentityCard(String identityCard);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );

    boolean existsByIdentityCardAndIdNot(
            String identityCard,
            Long id
    );

    List<Users> findByWarehouse_IdOrderByIdAsc(
            Long warehouseId
    );

    @Query("""
    SELECT u
    FROM Users u
    WHERE (
        :keyword IS NULL
        OR :keyword = ''
        OR LOWER(u.userCode)
               LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.name)
               LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email)
               LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.identityCard)
               LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (
        :roleId IS NULL
        OR EXISTS (
            SELECT ur.id
            FROM UserRoles ur
            WHERE ur.user = u
              AND ur.role.id = :roleId
        )
    )
    AND (
        :warehouseId IS NULL
        OR u.warehouse.id = :warehouseId
    )
    AND (
        :status IS NULL
        OR u.status = :status
    )
    """)
    Page<Users> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleId") Long roleId,
            @Param("warehouseId") Long warehouseId,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT u
    FROM Users u, UserRoles ur
    WHERE ur.user = u
      AND u.warehouse.id = :warehouseId
      AND ur.role.roleCode = :roleCode
    ORDER BY u.id ASC
    """)
    List<Users> findWarehouseUsersByRoleCode(
            @Param("warehouseId") Long warehouseId,
            @Param("roleCode") String roleCode
    );

    @Query(
            value = """
        SELECT MAX(
            CAST(SUBSTRING(user_code, 4) AS UNSIGNED)
        )
        FROM users
        WHERE user_code REGEXP '^USR[0-9]+$'
        """,
            nativeQuery = true
    )
    Long findMaxUserCodeNumber();
}