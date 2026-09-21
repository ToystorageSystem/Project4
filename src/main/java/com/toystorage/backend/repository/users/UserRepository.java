package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.UserStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository
        extends JpaRepository<Users, Long> {


    // =========================================================
    // LOGIN
    // =========================================================

    Optional<Users> findByEmail(
            String email
    );


    // =========================================================
    // UNIQUE EMAIL
    // =========================================================

    boolean existsByEmailIgnoreCase(
            String email
    );


    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );


    // =========================================================
    // UNIQUE IDENTITY CARD
    // =========================================================

    boolean existsByIdentityCard(
            String identityCard
    );


    boolean existsByIdentityCardAndIdNot(
            String identityCard,
            Long id
    );


    // =========================================================
    // AUTO USER CODE
    // =========================================================

    @Query(
            value = """
                    SELECT COALESCE(
                        MAX(
                            CAST(
                                SUBSTRING(user_code, 4)
                                AS UNSIGNED
                            )
                        ),
                        0
                    )
                    FROM users
                    WHERE user_code LIKE 'USR%'
                    """,
            nativeQuery = true
    )
    Long findMaxUserCodeNumber();


    // =========================================================
    // USER LIST + SEARCH + FILTER
    // =========================================================

    @Query("""
        SELECT DISTINCT u
        FROM Users u
        WHERE

            (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(u.name)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
                OR LOWER(u.email)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
                OR LOWER(u.userCode)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
            )

            AND (
                :status IS NULL
                OR u.status = :status
            )

            AND (
                :warehouseId IS NULL
                OR u.warehouse.id = :warehouseId
            )

            AND (
                :roleId IS NULL

                OR EXISTS (
                    SELECT ur.id
                    FROM UserRoles ur
                    WHERE ur.user.id = u.id
                    AND ur.role.id = :roleId
                )
            )
        """)
    Page<Users> searchUsers(

            @Param("keyword")
            String keyword,

            @Param("roleId")
            Long roleId,

            @Param("warehouseId")
            Long warehouseId,

            @Param("status")
            UserStatus status,

            Pageable pageable
    );


    // =========================================================
    // USERS ASSIGNED TO WAREHOUSE / STORE
    // =========================================================

    List<Users> findByWarehouse_IdOrderByIdAsc(
            Long warehouseId
    );


    // =========================================================
    // FIND MANAGER CURRENTLY ASSIGNED TO WAREHOUSE
    // =========================================================

    @Query("""
        SELECT DISTINCT u
        FROM Users u

        JOIN UserRoles ur
            ON ur.user.id = u.id

        WHERE u.warehouse.id = :warehouseId
          AND ur.role.roleCode = :roleCode
    """)
    List<Users> findWarehouseUsersByRoleCode(

            @Param("warehouseId")
            Long warehouseId,

            @Param("roleCode")
            String roleCode
    );
}