package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.Users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository
        extends JpaRepository<Users, Long> {


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
}